package com.enterpriseai.backend.ai.retrieval.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.exception.AiRetrievalException;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.DocumentType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final QueryEmbeddingService queryEmbeddingService;
    private final VectorStore vectorStore;
    private final RetrievalProperties properties;
    private final MeterRegistry meterRegistry;

    public SemanticSearchService(
            QueryEmbeddingService queryEmbeddingService,
            VectorStore vectorStore,
            RetrievalProperties properties,
            MeterRegistry meterRegistry) {
        this.queryEmbeddingService = queryEmbeddingService;
        this.vectorStore = vectorStore;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public List<RetrievedChunk> search(String query, Long workspaceId, Long userId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }

        log.info("QueryEmbeddingService: CALLED (query: '{}')", query);
        Timer.Sample embedSample = Timer.start(meterRegistry);
        List<Double> embedding = queryEmbeddingService.embedQuery(query);
        embedSample.stop(meterRegistry.timer("search.embedding.latency"));
        log.info("QueryEmbeddingService: Embedding generated? {}", embedding != null && !embedding.isEmpty());

        log.info("SemanticSearchService: CALLED");
        Timer.Sample vectorSample = Timer.start(meterRegistry);
        List<VectorSearchResult> results = searchVectorStore(embedding, workspaceId, userId);
        vectorSample.stop(meterRegistry.timer("search.vector.latency"));

        if (results == null) {
            log.info("SemanticSearchService: returned 0 chunks");
            return List.of();
        }

        List<RetrievedChunk> chunks = results.stream()
                .filter(java.util.Objects::nonNull)
                .filter(result -> result.score() >= properties.minimumSimilarityScore())
                .filter(result -> workspaceId.equals(longValue(result.metadata(), "workspaceId")))
                .map(this::toRetrievedChunk)
                .filter(java.util.Objects::nonNull)
                .toList();

        log.info("SemanticSearchService: returned {} chunks", chunks.size());
        
        log.info("==================================================");
        log.info("QUERY:");
        log.info(query);
        log.info("");
        log.info("WORKSPACE:");
        log.info(workspaceId.toString());
        log.info("");
        log.info("QUERY EMBEDDING DIMENSION: {}", embedding != null ? embedding.size() : "null");
        log.info("QDRANT FILTER: workspaceId = {}", workspaceId);
        log.info("QDRANT RESULTS COUNT: {}", chunks.size());
        log.info("RETRIEVED CHUNKS:");
        
        int index = 1;
        for (RetrievedChunk c : chunks) {
            log.info("{}.", index++);
            log.info("document = {}", c.documentFileName());
            log.info("documentId = {}", c.documentId());
            log.info("chunkId = {}", c.chunkId());
            log.info("score = {}", c.similarityScore());
            log.info("workspaceId = {}", c.workspaceId());
            log.info("text = \"{}\"", c.chunkText().replace("\n", " "));
            log.info("");
        }
        log.info("==================================================");
        
        return chunks;
    }

    private List<VectorSearchResult> searchVectorStore(List<Double> embedding, Long workspaceId, Long userId) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> vectorStore.search(embedding, properties.topK(), workspaceId, userId))
                    .orTimeout(properties.searchTimeout(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException) {
                throw new AiRetrievalException("Semantic search timed out", cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AiRetrievalException("Semantic search failed", cause);
        }
    }

    private RetrievedChunk toRetrievedChunk(VectorSearchResult result) {
        Map<String, Object> metadata = result.metadata();
        Long documentId = longValue(metadata, "documentId");
        Long workspaceId = longValue(metadata, "workspaceId");
        Long chunkId = result.chunkId() != null ? result.chunkId() : longValue(metadata, "chunkId");
        Long chunkIndex = longValue(metadata, "chunkIndex");
        String fileName = stringValue(metadata, "originalFileName");
        String documentTypeValue = stringValue(metadata, "documentType");
        String chunkText = stringValue(metadata, "chunkText");

        if (documentId == null || workspaceId == null || chunkId == null || chunkIndex == null
                || fileName == null || documentTypeValue == null || chunkText == null) {
            return null;
        }

        try {
            Long pageNumber = longValue(metadata, "pageNumber");
            return new RetrievedChunk(
                    documentId,
                    chunkId,
                    chunkIndex.intValue(),
                    pageNumber != null ? pageNumber.intValue() : null,
                    result.score(),
                    fileName,
                    DocumentType.valueOf(documentTypeValue),
                    chunkText,
                    workspaceId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Long longValue(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.valueOf(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        String value = metadata.get(key).toString();
        return value.isBlank() ? null : value;
    }
}
