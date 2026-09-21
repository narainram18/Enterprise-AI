package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import com.enterpriseai.backend.entity.DocumentType;

class SemanticSearchServiceIntegrationTest {

    @Test
    void realProviderNeutralSearchFlowFiltersRanksAndLimitsResults() {
        EmbeddingProvider provider = new EmbeddingProvider() {
            @Override
            public List<Double> embed(String text) {
                return List.of(1.0, 0.0);
            }

            @Override
            public List<List<Double>> embedBatch(List<String> texts) {
                return texts.stream().map(text -> List.of(1.0, 0.0)).toList();
            }
        };

        VectorStore vectorStore = new InMemoryVectorStore();
        SemanticSearchService service = new SemanticSearchService(
                new QueryEmbeddingService(provider),
                vectorStore,
                new RetrievalProperties(true, 3, 0.75, 2, 2, 1000, 1000, true, true, "STRICT"),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        List<RetrievedChunk> results = service.search("benefits", 7L, 1L);

        assertEquals(List.of(101L, 103L), results.stream().map(RetrievedChunk::chunkId).toList());
        assertEquals(List.of(0.98, 0.90), results.stream().map(RetrievedChunk::similarityScore).toList());
    }

    private static class InMemoryVectorStore implements VectorStore {
        @Override
        public void upsert(Long chunkId, List<Double> embedding, Map<String, Object> metadata) {
        }

        @Override
        public void upsertBatch(List<Long> chunkIds, List<List<Double>> embeddings,
                List<Map<String, Object>> metadatas) {
        }

        @Override
        public List<VectorSearchResult> search(List<Double> embedding, int topK, Long workspaceId, Long userId) {
            return new ArrayList<>(List.of(
                    result(101L, 0.98, 7L, "first"),
                    result(102L, 0.97, 8L, "other owner"),
                    result(103L, 0.90, 7L, "second"),
                    result(104L, 0.60, 7L, "below threshold"))).subList(0, topK);
        }

        @Override
        public void delete(Long chunkId) {
        }

        @Override
        public void deleteByDocument(Long documentId) {
        }

        private VectorSearchResult result(Long chunkId, double score, Long workspaceId, String text) {
            return new VectorSearchResult(chunkId, score, Map.of(
                    "documentId", 4L,
                    "workspaceId", workspaceId,
                    "chunkId", chunkId,
                    "chunkIndex", chunkId.intValue(),
                    "originalFileName", "handbook.txt",
                    "documentType", DocumentType.TXT.name(),
                    "chunkText", text));
        }
    }
}
