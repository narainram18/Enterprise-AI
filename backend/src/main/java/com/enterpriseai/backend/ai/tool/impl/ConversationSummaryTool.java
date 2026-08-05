package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.service.ConversationService;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

import org.springframework.context.annotation.Lazy;

@Component
public class ConversationSummaryTool implements Tool {

    private final ConversationService conversationService;
    private final AiProvider aiProvider;

    public ConversationSummaryTool(ConversationService conversationService, @Lazy AiProvider aiProvider) {
        this.conversationService = conversationService;
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "conversation_summary";
    }

    @Override
    public String getName() {
        return "Conversation Summary";
    }

    @Override
    public String getDescription() {
        return "Generates a brief summary of a past conversation by its ID.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("conversationId", "number", "The ID of the conversation to summarize.", true)
        );
    }

    @Override
    public String getCategory() {
        return "Conversation";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        Object convIdObj = parameters.get("conversationId");
        if (convIdObj == null) {
            return new ToolResult(false, "conversationId parameter is required.");
        }

        Long conversationId;
        try {
            conversationId = Long.valueOf(convIdObj.toString());
        } catch (NumberFormatException e) {
            return new ToolResult(false, "conversationId must be a valid number.");
        }

        try {
            List<ChatMessageResponse> messages = conversationService.getMessages(conversationId, context.email());
            if (messages == null || messages.isEmpty()) {
                return new ToolResult(true, "The conversation is empty.");
            }

            StringBuilder transcript = new StringBuilder();
            for (ChatMessageResponse msg : messages) {
                transcript.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
            }
            
            AiChatRequest request = new AiChatRequest(
                List.of(
                    new AiMessage(AiMessageRole.SYSTEM, "You are a helpful assistant. Summarize the following conversation transcript in 2-4 sentences."),
                    new AiMessage(AiMessageRole.USER, transcript.toString())
                ),
                0.3,
                0.9,
                null
            );
            
            String summary = aiProvider.generate(request);

            return new ToolResult(true, summary);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to summarize conversation: " + ex.getMessage());
        }
    }
}
