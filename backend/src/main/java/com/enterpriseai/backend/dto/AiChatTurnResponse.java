package com.enterpriseai.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiChatTurnResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage,
        List<RetrievalCitation> citations,
        RetrievalStatistics retrievalStatistics
) {
    public AiChatTurnResponse(ChatMessageResponse userMessage, ChatMessageResponse assistantMessage) {
        this(userMessage, assistantMessage, null, null);
    }
}
