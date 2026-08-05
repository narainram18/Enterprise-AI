package com.enterpriseai.backend.ai.tool;

public record ToolContext(
        Long workspaceId,
        String email,
        Long conversationId
) {
}
