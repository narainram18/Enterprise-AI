package com.enterpriseai.backend.ai.provider.ollama.dto;

public record OllamaChatStreamChunk(
        OllamaMessage message,
        boolean done
) {
}
