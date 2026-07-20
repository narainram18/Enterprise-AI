package com.enterpriseai.backend.ai.provider.ollama.dto;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream
) {
}
