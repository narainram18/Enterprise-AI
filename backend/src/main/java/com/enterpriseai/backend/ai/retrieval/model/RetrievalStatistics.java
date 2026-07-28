package com.enterpriseai.backend.ai.retrieval.model;

public record RetrievalStatistics(
        int retrievedChunks,
        int retrievedDocuments,
        boolean retrievalAttempted
) {
    public static RetrievalStatistics notAttempted() {
        return new RetrievalStatistics(0, 0, false);
    }
}
