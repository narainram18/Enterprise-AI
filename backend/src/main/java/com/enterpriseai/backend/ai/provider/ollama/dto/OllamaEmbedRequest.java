package com.enterpriseai.backend.ai.provider.ollama.dto;

import java.util.List;

public record OllamaEmbedRequest(
        String model,
        List<String> input
) {
}
