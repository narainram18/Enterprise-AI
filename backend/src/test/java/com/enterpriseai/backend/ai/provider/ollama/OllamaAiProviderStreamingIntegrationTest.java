package com.enterpriseai.backend.ai.provider.ollama;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;

@SpringBootTest
@EnabledIf("isOllamaAvailable")
class OllamaAiProviderStreamingIntegrationTest {

    @Autowired
    private AiProvider aiProvider;

    @Test
    void shouldReceiveAndCombineOllamaStreamingResponse() {
        List<String> tokens = new CopyOnWriteArrayList<>();

        boolean completed = aiProvider.stream(
                new AiChatRequest(List.of(
                        new AiMessage(
                                AiMessageRole.USER,
                                "Reply with a short explanation of dependency injection in Spring Boot."
                        )
                )),
                new AiStreamHandler() {
                    @Override
                    public void onToken(String token) {
                        tokens.add(token);
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                });

        String response = String.join("", tokens);
        System.out.println("AI STREAM RESPONSE: " + response);

        assertTrue(completed);
        assertFalse(tokens.isEmpty());
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
