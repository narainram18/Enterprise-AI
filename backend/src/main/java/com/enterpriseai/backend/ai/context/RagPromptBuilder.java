package com.enterpriseai.backend.ai.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import com.enterpriseai.backend.ai.config.AiChatProperties;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;

@Component
public class RagPromptBuilder {

    private static final String INSTRUCTIONS = "You are an enterprise assistant. "
            + "Never answer using unsupported facts. "
            + "Retrieved knowledge has higher priority than conversation history. "
            + "If retrieved knowledge conflicts with previous assistant replies, ignore the previous assistant replies. "
            + "Answer from retrieved knowledge whenever possible. If the answer is missing, explicitly say: \"I couldn't find this information in the uploaded documents.\" "
            + "Never fabricate document contents. Never mix retrieved facts with general knowledge unless explicitly asked.";

    private final TokenBudgetManager tokenBudgetManager;
    private final AiChatProperties properties;

    public RagPromptBuilder(TokenBudgetManager tokenBudgetManager, AiChatProperties properties) {
        this.tokenBudgetManager = tokenBudgetManager;
        this.properties = properties;
    }

    public AiChatRequest build(AiChatRequest historyRequest, String retrievalContext) {
        if (retrievalContext == null || retrievalContext.isBlank()) {
            return historyRequest;
        }

        String systemPrompt = INSTRUCTIONS;

        String currentQuestion = "";
        List<AiMessage> pastMessages = new ArrayList<>();

        if (historyRequest != null && historyRequest.messages() != null && !historyRequest.messages().isEmpty()) {
            List<AiMessage> allMessages = historyRequest.messages();
            currentQuestion = allMessages.get(allMessages.size() - 1).content();
            if (allMessages.size() > 1) {
                pastMessages = allMessages.subList(0, allMessages.size() - 1);
            }
        }

        List<AiMessage> truncatedHistory = tokenBudgetManager.truncateHistoryAi(
                pastMessages,
                systemPrompt,
                currentQuestion,
                retrievalContext,
                properties.maxTotalTokens(),
                properties.maxHistoryTokens()
        );

        StringBuilder userPrompt = new StringBuilder();

        if (!truncatedHistory.isEmpty()) {
            userPrompt.append("Conversation History\n");
            for (AiMessage msg : truncatedHistory) {
                String roleName = msg.role() == AiMessageRole.USER ? "User" : "Assistant";
                userPrompt.append(roleName).append(": ").append(msg.content()).append("\n\n");
            }
            userPrompt.append("--------------------------------\n");
        }

        if (retrievalContext != null && !retrievalContext.isBlank()) {
            userPrompt.append("Retrieved Knowledge\n\n");
            userPrompt.append(retrievalContext).append("\n");
            userPrompt.append("--------------------------------\n");
        }

        userPrompt.append("Current Question\n\n");
        userPrompt.append(currentQuestion);

        List<AiMessage> messages = new ArrayList<>();
        messages.add(new AiMessage(AiMessageRole.SYSTEM, systemPrompt));
        messages.add(new AiMessage(AiMessageRole.USER, userPrompt.toString()));

        return new AiChatRequest(List.copyOf(messages));
    }
}
