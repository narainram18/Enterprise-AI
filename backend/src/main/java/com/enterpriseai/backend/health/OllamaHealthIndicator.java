package com.enterpriseai.backend.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public OllamaHealthIndicator(@Value("${ai.ollama.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            // Ollama root endpoint returns 200 OK
            restClient.get().uri("/").retrieve().toBodilessEntity();
            return Health.up().withDetail("service", "Ollama").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("service", "Ollama").build();
        }
    }
}
