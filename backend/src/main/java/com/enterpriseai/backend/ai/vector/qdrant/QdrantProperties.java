package com.enterpriseai.backend.ai.vector.qdrant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.vector.qdrant")
public record QdrantProperties(
        String host,
        int port,
        String collection,
        String apiKey
) {
    public QdrantProperties {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 6334;
        }
        if (collection == null || collection.isBlank()) {
            collection = "documents";
        }
    }
}
