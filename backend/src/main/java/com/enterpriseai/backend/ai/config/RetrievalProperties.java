package com.enterpriseai.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "ai.retrieval")
public record RetrievalProperties(
        boolean enabled,
        int topK,
        double minimumSimilarityScore,
        int maximumRetrievedChunks,
        int maximumRetrievedDocuments,
        int maximumRetrievedCharacters,
        long searchTimeout
) {
    public RetrievalProperties(
            int topK,
            double minimumSimilarityScore,
            int maximumRetrievedChunks,
            long searchTimeout) {
        this(true, topK, minimumSimilarityScore, maximumRetrievedChunks, 5, 12000, searchTimeout);
    }

    @ConstructorBinding
    public RetrievalProperties(
            boolean enabled,
            int topK,
            double minimumSimilarityScore,
            int maximumRetrievedChunks,
            int maximumRetrievedDocuments,
            int maximumRetrievedCharacters,
            long searchTimeout) {
        if (topK <= 0) {
            topK = 10;
        }
        if (!Double.isFinite(minimumSimilarityScore)
                || minimumSimilarityScore < 0.0
                || minimumSimilarityScore > 1.0) {
            minimumSimilarityScore = 0.5;
        }
        if (maximumRetrievedChunks <= 0) {
            maximumRetrievedChunks = 5;
        }
        if (maximumRetrievedDocuments <= 0) {
            maximumRetrievedDocuments = maximumRetrievedChunks;
        }
        if (maximumRetrievedCharacters <= 0) {
            maximumRetrievedCharacters = 12000;
        }
        if (searchTimeout <= 0) {
            searchTimeout = 5000;
        }
        this.enabled = enabled;
        this.topK = topK;
        this.minimumSimilarityScore = minimumSimilarityScore;
        this.maximumRetrievedChunks = maximumRetrievedChunks;
        this.maximumRetrievedDocuments = maximumRetrievedDocuments;
        this.maximumRetrievedCharacters = maximumRetrievedCharacters;
        this.searchTimeout = searchTimeout;
    }
}
