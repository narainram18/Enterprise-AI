package com.enterpriseai.backend.ai.provider.ollama;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.enterpriseai.backend.ai.config.OllamaProperties;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.exception.AiStreamCancelledException;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaChatRequest;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaChatResponse;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaChatStreamChunk;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.ai.exception.AiGenerationException;

@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

    private final RestClient restClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaAiProvider(
            RestClient.Builder restClientBuilder,
            OllamaProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public String generate(AiChatRequest request) {

        Map<String, Object> options = new java.util.HashMap<>();
        if (request.temperature() != null) {
            options.put("temperature", request.temperature());
        }
        if (request.topP() != null) {
            options.put("top_p", request.topP());
        }

        String model = request.model() != null && !request.model().isBlank() 
                ? request.model() 
                : properties.model();

        OllamaChatRequest ollamaRequest = new OllamaChatRequest(
                model,
                request.messages().stream()
                        .map(this::toOllamaMessage)
                        .toList(),
                false,
                false,
                options.isEmpty() ? null : options
        );
        
        log.info("Prompt sent to Ollama:");
        for (AiMessage msg : request.messages()) {
            log.info("{}:\n{}", msg.role(), msg.content());
        }

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

    @Override
    public boolean stream(AiChatRequest request, AiStreamHandler handler) {

        Map<String, Object> options = new java.util.HashMap<>();
        if (request.temperature() != null) {
            options.put("temperature", request.temperature());
        }
        if (request.topP() != null) {
            options.put("top_p", request.topP());
        }

        String model = request.model() != null && !request.model().isBlank() 
                ? request.model() 
                : properties.model();

        OllamaChatRequest ollamaRequest = new OllamaChatRequest(
                model,
                request.messages().stream()
                        .map(this::toOllamaMessage)
                        .toList(),
                true,
                false,
                options.isEmpty() ? null : options
        );

        boolean[] completed = {false};
        log.info("Prompt sent to Ollama:");
        for (AiMessage msg : request.messages()) {
            log.info("{}:\n{}", msg.role(), msg.content());
        }
        
        try {
            log.info("Ollama JSON Payload: {}", objectMapper.writeValueAsString(ollamaRequest));
        } catch (Exception e) {
            log.error("Failed to serialize Ollama request", e);
        }

        try {
            restClient
                    .post()
                    .uri("/api/chat")
                    .body(ollamaRequest)
                    .exchange((clientRequest, clientResponse) -> {
                        if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                            String body = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            log.error("Ollama Stream HTTP Error: {} {}", clientResponse.getStatusCode().value(), body);
                            throw providerResponseException(clientResponse.getStatusCode().value());
                        }

                        if (clientResponse.getBody() == null) {
                            throw new AiGenerationException(
                                    "AI provider returned an empty stream");
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(
                                        clientResponse.getBody(),
                                        StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (handler.isCancelled()) {
                                    return null;
                                }
                                if (line.isBlank()) {
                                    continue;
                                }

                                OllamaChatStreamChunk chunk = readChunk(line);
                                if (chunk.message() != null &&
                                        chunk.message().content() != null &&
                                        !chunk.message().content().isEmpty()) {
                                    handler.onToken(chunk.message().content());
                                }

                                if (chunk.done()) {
                                    completed[0] = true;
                                    log.info("Ollama done=true received");
                                    break;
                                }
                            }
                        } catch (AiStreamCancelledException ex) {
                            return null;
                        } catch (IOException ex) {
                            throw new AiGenerationException(
                                    "AI provider stream failed",
                                    ex);
                        }
                        return null;
                    });
        } catch (AiStreamCancelledException ex) {
            return false;
        } catch (AiGenerationException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw providerResponseException(ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new AiGenerationException("AI provider is unavailable", ex);
        } catch (RestClientException ex) {
            throw new AiGenerationException("AI provider stream failed", ex);
        }

        if (!handler.isCancelled() && !completed[0]) {
            throw new AiGenerationException(
                    "AI provider stream ended unexpectedly");
        }

        return completed[0] && !handler.isCancelled();
    }

    private OllamaChatStreamChunk readChunk(String line) {
        try {
            return objectMapper.readValue(line, OllamaChatStreamChunk.class);
        } catch (JsonProcessingException ex) {
            throw new AiGenerationException(
                    "AI provider returned a malformed stream",
                    ex);
        }
    }

    private AiGenerationException providerResponseException(int statusCode) {
        return providerResponseException(statusCode, null);
    }

    private AiGenerationException providerResponseException(
            int statusCode,
            Throwable cause) {
        String message;
        if (cause instanceof RestClientResponseException rce) {
            log.error("Ollama HTTP Error: {} {}", statusCode, rce.getResponseBodyAsString());
        } else {
            log.error("Ollama Error: {} {}", statusCode, cause);
        }
        if (statusCode == 404) {
            message = "AI model is unavailable";
        } else if (statusCode >= 400 && statusCode < 500) {
            message = "AI provider rejected the request";
        } else {
            message = "AI provider request failed";
        }
        return cause == null
                ? new AiGenerationException(message)
                : new AiGenerationException(message, cause);
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
