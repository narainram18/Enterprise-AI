package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;
import com.enterpriseai.backend.entity.MessageRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private Long id;
    private MessageRole role;
    private String content;
    private LocalDateTime createdAt;
    private List<RetrievalCitation> citations;
    private RetrievalStatistics retrievalStatistics;

    public ChatMessageResponse(Long id, MessageRole role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }
}
