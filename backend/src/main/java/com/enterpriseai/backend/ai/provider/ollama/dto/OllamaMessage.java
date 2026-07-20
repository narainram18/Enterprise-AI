package com.enterpriseai.backend.ai.provider.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaMessage(
        String role,
        String content,
        String thinking
) {

    public OllamaMessage(String role, String content) {
        this(role, content, null);
    }
}
