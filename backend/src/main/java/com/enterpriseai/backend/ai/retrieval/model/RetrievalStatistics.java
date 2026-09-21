package com.enterpriseai.backend.ai.retrieval.model;

public record RetrievalStatistics(
        int retrievedChunks,
        int retrievedDocuments,
        boolean retrievalAttempted,
        String query,
        String retrievalMethod,
        long retrievalLatencyMs,
        long generationLatencyMs,
        int semanticResultsCount,
        int keywordResultsCount
) {
    public static RetrievalStatistics notAttempted() {
        return new RetrievalStatistics(0, 0, false, null, null, 0, 0, 0, 0);
    }
}
