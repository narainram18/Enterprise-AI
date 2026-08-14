package com.enterpriseai.backend.mapper;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.dto.AgentTaskResponse;
import com.enterpriseai.backend.entity.AgentTask;

@Component
public class AgentTaskMapper {

    public AgentTaskResponse toResponse(AgentTask task) {
        if (task == null) {
            return null;
        }

        return new AgentTaskResponse(
                task.getId(),
                task.getAgentId(),
                task.getCreatedBy(),
                task.getPrompt(),
                task.getStatus().name(),
                task.getResult(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
