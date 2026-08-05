package com.enterpriseai.backend.ai.model;

import java.util.List;

public record AiChatRequest(
        List<AiMessage> messages,
        Double temperature,
        Double topP,
        String model
) {
}
