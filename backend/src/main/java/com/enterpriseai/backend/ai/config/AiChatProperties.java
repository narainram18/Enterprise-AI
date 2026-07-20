package com.enterpriseai.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.chat")
public record AiChatProperties(
        int maxContextMessages
) {
}
