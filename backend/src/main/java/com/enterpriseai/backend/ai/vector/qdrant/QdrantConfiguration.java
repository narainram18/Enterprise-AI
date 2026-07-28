package com.enterpriseai.backend.ai.vector.qdrant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
@ConditionalOnProperty(name = "ai.vector.provider", havingValue = "qdrant")
public class QdrantConfiguration {

    @Bean
    public QdrantClient qdrantClient(QdrantProperties properties) {
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                properties.host(),
                properties.port(),
                false // useTls = false for sensible default local dev
        );

        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            grpcClientBuilder.withApiKey(properties.apiKey());
        }

        return new QdrantClient(grpcClientBuilder.build());
    }
}
