package com.enterpriseai.backend.ai.retrieval.model;

import com.enterpriseai.backend.entity.DocumentType;

public record RetrievedChunk(
        Long documentId,
        Long chunkId,
        int chunkIndex,
        Integer pageNumber,
        double similarityScore,
        String documentFileName,
        DocumentType documentType,
        String chunkText,
        Long workspaceId
) {
}
