package com.enterpriseai.backend.ai.vector.dto;

import java.util.Map;

public record VectorSearchResult(
        Long chunkId,
        double score,
        Map<String, Object> metadata
) {
}
