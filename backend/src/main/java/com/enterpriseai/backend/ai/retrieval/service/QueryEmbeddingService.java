package com.enterpriseai.backend.ai.retrieval.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;

@Service
public class QueryEmbeddingService {

    private final EmbeddingProvider embeddingProvider;

    public QueryEmbeddingService(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public List<Double> embedQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be null or blank");
        }

        List<Double> embedding = embeddingProvider.embed(query);
        if (embedding == null || embedding.isEmpty()) {
            throw new AiEmbeddingException("AI provider returned an invalid query embedding");
        }
        if (embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new AiEmbeddingException("AI provider returned a query embedding with invalid values");
        }
        return embedding;
    }
}
