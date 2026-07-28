package com.enterpriseai.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EnabledIf("isOllamaAvailable")
class OllamaEmbeddingProviderIntegrationTest {

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Value("${ai.embedding.ollama.model}")
    private String configuredModel;

    @Test
    void shouldGenerateEmbeddingFromOllama() {
        List<Double> embedding = embeddingProvider.embed("Enterprise AI Workspace test");

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());

        System.out.println("EMBEDDING MODEL: " + configuredModel);
        System.out.println("VECTOR DIMENSION: " + embedding.size());
    }

    static boolean isOllamaAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }
}
