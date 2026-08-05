package com.enterpriseai.backend.ai.retrieval.model;

public record RetrievalCitation(
        Long documentId,
        String fileName,
        int chunkIndex,
        Integer pageNumber,
        double similarityScore
) {
}
