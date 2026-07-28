package com.enterpriseai.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.embedding.ollama")
public record OllamaEmbeddingProperties(
        String baseUrl,
        String model
) {
}
