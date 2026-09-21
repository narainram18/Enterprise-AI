package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.DocumentType;

class SemanticSearchServiceTest {

    private QueryEmbeddingService queryEmbeddingService;
    private VectorStore vectorStore;
    private SemanticSearchService service;

    @BeforeEach
    void setUp() {
        EmbeddingProvider provider = org.mockito.Mockito.mock(EmbeddingProvider.class);
        queryEmbeddingService = new QueryEmbeddingService(provider);
        vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        service = new SemanticSearchService(
                queryEmbeddingService,
                vectorStore,
                new RetrievalProperties(true, 3, 0.70, 2, 2, 1000, 1000, true, true, "STRICT"),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        when(provider.embed("benefits")).thenReturn(List.of(0.1, 0.2));
    }

    @Test
    void filtersByOwnerAndMinimumScore() {
        when(vectorStore.search(anyList(), eq(3), eq(7L), org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of(
                result(11L, 0.95, 7L, 4L, 0, "first"),
                result(12L, 0.90, 8L, 5L, 0, "other owner"),
                result(13L, 0.69, 7L, 4L, 1, "below threshold"),
                result(14L, 0.85, 7L, 4L, 2, "second"),
                result(15L, 0.80, 7L, 4L, 3, "truncated")));

        List<RetrievedChunk> results = service.search("benefits", 7L, 1L);

        assertEquals(3, results.size());
        assertEquals(List.of(11L, 14L, 15L), results.stream().map(RetrievedChunk::chunkId).toList());
        assertTrue(results.stream().allMatch(result -> result.workspaceId().equals(7L)));
        verify(vectorStore).search(List.of(0.1, 0.2), 3, 7L, 1L);
    }

    @Test
    void skipsResultsWithMissingOrInvalidMetadata() {
        when(vectorStore.search(anyList(), eq(3), eq(7L), org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of(
                new VectorSearchResult(20L, 0.99, Map.of("workspaceId", 7L)),
                result(21L, 0.98, 7L, 4L, 0, "valid")));

        List<RetrievedChunk> results = service.search("benefits", 7L, 1L);

        assertEquals(List.of(21L), results.stream().map(RetrievedChunk::chunkId).toList());
    }

    private VectorSearchResult result(
            Long chunkId, double score, Long workspaceId, Long documentId, int chunkIndex, String content) {
        return new VectorSearchResult(
                chunkId,
                score,
                Map.of(
                        "workspaceId", workspaceId,
                        "documentId", documentId,
                        "chunkId", chunkId,
                        "chunkIndex", chunkIndex,
                        "originalFileName", "handbook.txt",
                        "documentType", DocumentType.TXT.name(),
                        "chunkText", content));
    }
}
