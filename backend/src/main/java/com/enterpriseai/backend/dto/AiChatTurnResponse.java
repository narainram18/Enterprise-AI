package com.enterpriseai.backend.dto;

public record AiChatTurnResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage
) {
}
