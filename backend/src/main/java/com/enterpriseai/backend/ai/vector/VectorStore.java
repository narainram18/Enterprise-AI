package com.enterpriseai.backend.ai.vector;

import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void upsert(Long chunkId, List<Double> embedding, Map<String, Object> metadata);

    void upsertBatch(List<Long> chunkIds, List<List<Double>> embeddings, List<Map<String, Object>> metadatas);

    List<VectorSearchResult> search(List<Double> embedding, int topK, Long workspaceId, Long userId);

    void delete(Long chunkId);

    void deleteByDocument(Long documentId);
}
