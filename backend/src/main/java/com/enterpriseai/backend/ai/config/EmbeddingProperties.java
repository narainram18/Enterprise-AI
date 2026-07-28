package com.enterpriseai.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.embedding")
public record EmbeddingProperties(
        String provider,
        int batchSize
) {
    public EmbeddingProperties {
        if (batchSize <= 0) {
            batchSize = 32;
        }
    }
}
