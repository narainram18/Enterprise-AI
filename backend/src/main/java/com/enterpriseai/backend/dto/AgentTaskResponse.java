package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;

public record AgentTaskResponse(
        Long id,
        String agentId,
        String createdBy,
        String prompt,
        String status,
        String result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
