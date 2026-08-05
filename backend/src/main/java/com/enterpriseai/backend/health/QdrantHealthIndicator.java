package com.enterpriseai.backend.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@Component
public class QdrantHealthIndicator implements HealthIndicator {

    private final QdrantClient qdrantClient;

    public QdrantHealthIndicator(
            @Value("${ai.vector.qdrant.host}") String host,
            @Value("${ai.vector.qdrant.port}") int port) {
        this.qdrantClient = new QdrantClient(QdrantGrpcClient.newBuilder(host, port, false).build());
    }

    @Override
    public Health health() {
        try {
            Object reply = qdrantClient.healthCheckAsync().get();
            return Health.up().withDetail("service", "Qdrant").withDetail("reply", reply.toString()).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("service", "Qdrant").build();
        }
    }
}
