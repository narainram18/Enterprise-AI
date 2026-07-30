package com.enterpriseai.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.chat")
public record AiChatProperties(
        int maxContextMessages,
        long streamTimeout,
        int maxHistoryTokens,
        int maxContextTokens,
        int maxTotalTokens
) {
    public AiChatProperties {
        if (maxHistoryTokens <= 0) maxHistoryTokens = 1000;
        if (maxContextTokens <= 0) maxContextTokens = 2000;
        if (maxTotalTokens <= 0) maxTotalTokens = 4096;
    }
}
