package com.enterpriseai.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EnabledIf("isOllamaAvailable")
class OllamaAiProviderIntegrationTest {

    @Autowired
    private AiProvider aiProvider;

    @Test
    void shouldGenerateResponseFromOllama() {

        String response = aiProvider.generate(
                new AiChatRequest(List.of(
                        new AiMessage(
                                AiMessageRole.USER,
                                "Reply with exactly these two words: OLLAMA WORKS"
                        )
                ))
        );

        System.out.println("AI RESPONSE: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
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
