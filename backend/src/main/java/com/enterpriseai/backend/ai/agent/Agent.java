package com.enterpriseai.backend.ai.agent;

public record Agent(
        String id,
        String name,
        String description,
        String icon,
        String color,
        String systemPrompt,
        Double temperature,
        Double topP,
        String model,
        boolean supportsRag,
        boolean supportsStreaming,
        java.util.List<String> supportedTools
) {
}
