package com.enterpriseai.backend.ai.provider.ollama;

import java.util.List;

import com.enterpriseai.backend.ai.config.OllamaEmbeddingProperties;
import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaEmbedRequest;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaEmbedResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final OllamaEmbeddingProperties properties;

    public OllamaEmbeddingProvider(
            RestClient.Builder restClientBuilder,
            OllamaEmbeddingProperties properties
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }
        
        List<List<Double>> embeddings = embedBatch(List.of(text));
        
        if (embeddings == null || embeddings.isEmpty() || embeddings.get(0) == null || embeddings.get(0).isEmpty()) {
            throw new AiEmbeddingException("AI provider returned an empty embedding");
        }
        
        return embeddings.get(0);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Texts to embed must not be null or empty");
        }
        
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Text to embed must not be null or blank");
            }
        }

        OllamaEmbedRequest ollamaRequest = new OllamaEmbedRequest(
                properties.model(),
                texts
        );

        try {
            OllamaEmbedResponse response = restClient
                    .post()
                    .uri("/api/embed")
                    .body(ollamaRequest)
                    .retrieve()
                    .body(OllamaEmbedResponse.class);

            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new AiEmbeddingException("AI provider returned an invalid response");
            }

            validateEmbeddings(texts, response.embeddings());

            return response.embeddings();
        } catch (AiEmbeddingException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new AiEmbeddingException("Embedding model is unavailable", ex);
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new AiEmbeddingException("AI provider rejected the request", ex);
            }
            throw new AiEmbeddingException("AI provider request failed", ex);
        } catch (ResourceAccessException ex) {
            throw new AiEmbeddingException("AI provider is unavailable", ex);
        } catch (RestClientException ex) {
            throw new AiEmbeddingException("AI embedding generation failed", ex);
        }
    }

    private void validateEmbeddings(List<String> texts, List<List<Double>> embeddings) {
        if (embeddings.size() != texts.size()) {
            throw new AiEmbeddingException("AI provider returned an embedding count mismatch");
        }

        int dimension = -1;
        for (List<Double> embedding : embeddings) {
            if (embedding == null || embedding.isEmpty()) {
                throw new AiEmbeddingException("AI provider returned an invalid vector");
            }
            if (dimension < 0) {
                dimension = embedding.size();
            } else if (embedding.size() != dimension) {
                throw new AiEmbeddingException("AI provider returned inconsistent vector dimensions");
            }
            if (embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
                throw new AiEmbeddingException("AI provider returned a vector with invalid values");
            }
        }
    }
}
