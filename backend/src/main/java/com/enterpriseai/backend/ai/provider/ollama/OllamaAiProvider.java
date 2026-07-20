package com.enterpriseai.backend.ai.provider.ollama;

import com.enterpriseai.backend.ai.config.OllamaProperties;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaChatRequest;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaChatResponse;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.enterpriseai.backend.ai.exception.AiGenerationException;

@Component
public class OllamaAiProvider implements AiProvider {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaAiProvider(
            RestClient.Builder restClientBuilder,
            OllamaProperties properties
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public String generate(AiChatRequest request) {

        OllamaChatRequest ollamaRequest = new OllamaChatRequest(
                properties.model(),
                request.messages().stream()
                        .map(this::toOllamaMessage)
                        .toList(),
                false
        );

        try {
            OllamaChatResponse response = restClient
                    .post()
                    .uri("/api/chat")
                    .body(ollamaRequest)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null ||
                    response.message() == null ||
                    response.message().content() == null ||
                    response.message().content().isBlank()) {
                throw new AiGenerationException(
                        "AI provider returned an invalid response");
            }

            return response.message().content();
        } catch (AiGenerationException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new AiGenerationException("AI model is unavailable", ex);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new AiGenerationException(
                        "AI provider rejected the request",
                        ex);
            }
            throw new AiGenerationException("AI provider request failed", ex);
        } catch (ResourceAccessException ex) {
            throw new AiGenerationException("AI provider is unavailable", ex);
        } catch (RestClientException ex) {
            throw new AiGenerationException("AI generation failed", ex);
        }
    }

    private OllamaMessage toOllamaMessage(AiMessage message) {
        return new OllamaMessage(toOllamaRole(message.role()), message.content());
    }

    private String toOllamaRole(AiMessageRole role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }
}
