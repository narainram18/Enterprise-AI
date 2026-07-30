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

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final QueryEmbeddingService queryEmbeddingService;
    private final VectorStore vectorStore;
    private final RetrievalProperties properties;

    public SemanticSearchService(
            QueryEmbeddingService queryEmbeddingService,
            VectorStore vectorStore,
            RetrievalProperties properties) {
        this.queryEmbeddingService = queryEmbeddingService;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<RetrievedChunk> search(String query, Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner ID must not be null");
        }

        log.info("QueryEmbeddingService: CALLED (query: '{}')", query);
        List<Double> embedding = queryEmbeddingService.embedQuery(query);
        log.info("QueryEmbeddingService: Embedding generated? {}", embedding != null && !embedding.isEmpty());

        log.info("SemanticSearchService: CALLED");
        List<VectorSearchResult> results = searchVectorStore(embedding);

        if (results == null) {
            log.info("SemanticSearchService: returned 0 chunks");
            return List.of();
        }

        List<RetrievedChunk> chunks = results.stream()
                .filter(java.util.Objects::nonNull)
                .filter(result -> result.score() >= properties.minimumSimilarityScore())
                .filter(result -> ownerId.equals(longValue(result.metadata(), "ownerId")))
                .map(this::toRetrievedChunk)
                .filter(java.util.Objects::nonNull)
                .limit(properties.maximumRetrievedChunks())
                .toList();

        log.info("SemanticSearchService: returned {} chunks", chunks.size());
        if (!chunks.isEmpty()) {
            log.info("Top similarity: {}", chunks.get(0).similarityScore());
            for (RetrievedChunk c : chunks) {
                log.info("Retrieved chunk ID: {}, similarity: {}", c.chunkId(), c.similarityScore());
                log.info("Chunk:\n{}", c.chunkText());
            }
        }
        
        return chunks;
    }

    private List<VectorSearchResult> searchVectorStore(List<Double> embedding) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> vectorStore.search(embedding, properties.topK()))
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
        Long ownerId = longValue(metadata, "ownerId");
        Long chunkId = result.chunkId() != null ? result.chunkId() : longValue(metadata, "chunkId");
        Long chunkIndex = longValue(metadata, "chunkIndex");
        String fileName = stringValue(metadata, "originalFileName");
        String documentTypeValue = stringValue(metadata, "documentType");
        String chunkText = stringValue(metadata, "chunkText");

        if (documentId == null || ownerId == null || chunkId == null || chunkIndex == null
                || fileName == null || documentTypeValue == null || chunkText == null) {
            return null;
        }

        try {
            return new RetrievedChunk(
                    documentId,
                    chunkId,
                    chunkIndex.intValue(),
                    result.score(),
                    fileName,
                    DocumentType.valueOf(documentTypeValue),
                    chunkText,
                    ownerId);
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
