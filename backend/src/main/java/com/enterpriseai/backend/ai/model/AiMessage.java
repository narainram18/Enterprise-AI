package com.enterpriseai.backend.ai.model;

public record AiMessage(
        AiMessageRole role,
        String content
) {
}
