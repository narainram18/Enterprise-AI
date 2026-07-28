package com.enterpriseai.backend.ai.provider.ollama.dto;

import java.util.List;

public record OllamaEmbedResponse(
        String model,
        List<List<Double>> embeddings
) {
}
