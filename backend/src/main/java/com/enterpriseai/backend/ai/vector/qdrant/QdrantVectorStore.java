package com.enterpriseai.backend.ai.vector.qdrant;

import com.enterpriseai.backend.ai.exception.AiVectorStoreException;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@ConditionalOnProperty(name = "ai.vector.provider", havingValue = "qdrant")
public class QdrantVectorStore implements VectorStore {

    private final QdrantClient qdrantClient;
    private final QdrantProperties properties;
    private volatile boolean collectionInitialized = false;

    public QdrantVectorStore(QdrantClient qdrantClient, QdrantProperties properties) {
        this.qdrantClient = qdrantClient;
        this.properties = properties;
    }

    private void ensureCollectionExists(int dimension) {
        if (collectionInitialized) {
            return;
        }

        synchronized (this) {
            if (collectionInitialized) {
                return;
            }
            try {
                boolean exists = qdrantClient.collectionExistsAsync(properties.collection()).get();
                if (!exists) {
                    VectorParams vectorParams = VectorParams.newBuilder()
                            .setSize(dimension)
                            .setDistance(Distance.Cosine)
                            .build();

                    qdrantClient.createCollectionAsync(properties.collection(), vectorParams).get();
                }
                collectionInitialized = true;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new AiVectorStoreException("Failed to initialize Qdrant collection", e);
            }
        }
    }

    @Override
    public void upsert(Long chunkId, List<Double> embedding, Map<String, Object> metadata) {
        upsertBatch(List.of(chunkId), List.of(embedding), List.of(metadata));
    }

    @Override
    public void upsertBatch(List<Long> chunkIds, List<List<Double>> embeddings, List<Map<String, Object>> metadatas) {
        validateBatch(chunkIds, embeddings, metadatas);
        if (chunkIds.isEmpty()) return;

        ensureCollectionExists(embeddings.get(0).size());

        List<PointStruct> points = new ArrayList<>();
        for (int i = 0; i < chunkIds.size(); i++) {
            PointId pointId = PointIdFactory.id(chunkIds.get(i));
            
            float[] floatArray = new float[embeddings.get(i).size()];
            for (int j = 0; j < embeddings.get(i).size(); j++) {
                floatArray[j] = embeddings.get(i).get(j).floatValue();
            }

            PointStruct.Builder pointBuilder = PointStruct.newBuilder()
                    .setId(pointId)
                    .setVectors(VectorsFactory.vectors(floatArray));

            Map<String, Object> metadata = metadatas.get(i);
            if (metadata != null) {
                Map<String, Value> payloadMap = mapToQdrantValue(metadata);
                pointBuilder.putAllPayload(payloadMap);
            }

            points.add(pointBuilder.build());
        }

        try {
            qdrantClient.upsertAsync(properties.collection(), points).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiVectorStoreException("Failed to upsert batch to Qdrant", e);
        }
    }

    @Override
    public List<VectorSearchResult> search(List<Double> embedding, int topK, Long workspaceId) {
        ensureCollectionExists(embedding.size());

        float[] floatArray = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            floatArray[i] = embedding.get(i).floatValue();
        }

        SearchPoints.Builder requestBuilder = SearchPoints.newBuilder()
                .setCollectionName(properties.collection())
                .addAllVector(java.util.stream.IntStream.range(0, floatArray.length).mapToObj(i -> floatArray[i]).toList())
                .setLimit(topK)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build());

        if (workspaceId != null) {
            Filter filter = Filter.newBuilder()
                    .addMust(ConditionFactory.match("workspaceId", workspaceId))
                    .build();
            requestBuilder.setFilter(filter);
        }

        SearchPoints request = requestBuilder.build();

        try {
            var searchResults = qdrantClient.searchAsync(request).get();
            return searchResults.stream().map(result -> {
                if (!result.getId().hasNum()) {
                    throw new AiVectorStoreException("Qdrant returned a non-numeric point identifier");
                }
                Long chunkId = result.getId().getNum();
                double score = result.getScore();
                Map<String, Object> metadata = qdrantValueToMap(result.getPayloadMap());
                return new VectorSearchResult(chunkId, score, metadata);
            }).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiVectorStoreException("Failed to search in Qdrant", e);
        }
    }

    @Override
    public void delete(Long chunkId) {
        if (!collectionInitialized) return;
        
        try {
            qdrantClient.deleteAsync(properties.collection(), List.of(PointIdFactory.id(chunkId))).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiVectorStoreException("Failed to delete point in Qdrant", e);
        }
    }

    @Override
    public void deleteByDocument(Long documentId) {
        if (!collectionInitialized) return;

        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.match("documentId", documentId))
                .build();

        try {
            qdrantClient.deleteAsync(properties.collection(), filter).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiVectorStoreException("Failed to delete by document in Qdrant", e);
        }
    }

    private void validateBatch(
            List<Long> chunkIds,
            List<List<Double>> embeddings,
            List<Map<String, Object>> metadatas) {
        if (chunkIds == null || embeddings == null || metadatas == null
                || chunkIds.size() != embeddings.size()
                || chunkIds.size() != metadatas.size()) {
            throw new IllegalArgumentException("Vector batch sizes must match");
        }
        for (int i = 0; i < chunkIds.size(); i++) {
            if (chunkIds.get(i) == null || embeddings.get(i) == null || embeddings.get(i).isEmpty()) {
                throw new IllegalArgumentException("Vector batch contains an invalid item");
            }
        }
    }

    private Map<String, Value> mapToQdrantValue(Map<String, Object> map) {
        Map<String, Value> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if (obj == null) {
                result.put(entry.getKey(), Value.newBuilder().setNullValue(io.qdrant.client.grpc.JsonWithInt.NullValue.NULL_VALUE).build());
            } else if (obj instanceof String s) {
                result.put(entry.getKey(), ValueFactory.value(s));
            } else if (obj instanceof Integer i) {
                result.put(entry.getKey(), ValueFactory.value(i));
            } else if (obj instanceof Long l) {
                result.put(entry.getKey(), ValueFactory.value(l));
            } else if (obj instanceof Double d) {
                result.put(entry.getKey(), ValueFactory.value(d));
            } else if (obj instanceof Boolean b) {
                result.put(entry.getKey(), ValueFactory.value(b));
            } else {
                result.put(entry.getKey(), ValueFactory.value(obj.toString()));
            }
        }
        return result;
    }

    private Map<String, Object> qdrantValueToMap(Map<String, Value> payloadMap) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Value> entry : payloadMap.entrySet()) {
            Value val = entry.getValue();
            switch (val.getKindCase()) {
                case STRING_VALUE -> result.put(entry.getKey(), val.getStringValue());
                case INTEGER_VALUE -> result.put(entry.getKey(), val.getIntegerValue());
                case DOUBLE_VALUE -> result.put(entry.getKey(), val.getDoubleValue());
                case BOOL_VALUE -> result.put(entry.getKey(), val.getBoolValue());
                case NULL_VALUE -> result.put(entry.getKey(), null);
                default -> result.put(entry.getKey(), "");
            }
        }
        return result;
    }
}
