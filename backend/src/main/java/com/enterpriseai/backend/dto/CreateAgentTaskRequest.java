package com.enterpriseai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentTaskRequest(
        @NotBlank(message = "Agent ID is required")
        String agentId,
        @NotBlank(message = "Prompt is required")
        String prompt
) {
}
