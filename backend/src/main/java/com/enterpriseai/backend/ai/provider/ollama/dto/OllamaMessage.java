package com.enterpriseai.backend.ai.provider.ollama.dto;

public record OllamaMessage(
        String role,
        String content
) {
}
