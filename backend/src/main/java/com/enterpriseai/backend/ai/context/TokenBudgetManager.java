package com.enterpriseai.backend.ai.context;

import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.entity.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TokenBudgetManager {

    private static final int CHARS_PER_TOKEN = 4;

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / CHARS_PER_TOKEN;
    }

    public List<ChatMessage> truncateHistory(
            List<ChatMessage> history,
            String systemPrompt,
            String currentQuestion,
            String retrievedKnowledge,
            int maxTotalTokens,
            int maxHistoryTokens) {

        int sysTokens = estimateTokens(systemPrompt);
        int qTokens = estimateTokens(currentQuestion);
        int ragTokens = estimateTokens(retrievedKnowledge);

        int availableTokensForHistory = maxTotalTokens - (sysTokens + qTokens + ragTokens);
        int allowedHistoryTokens = Math.min(availableTokensForHistory, maxHistoryTokens);

        if (allowedHistoryTokens <= 0 || history.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> retained = new ArrayList<>();
        int currentHistoryTokens = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            int msgTokens = estimateTokens(msg.getContent()) + 10; 

            if (currentHistoryTokens + msgTokens <= allowedHistoryTokens) {
                retained.add(msg);
                currentHistoryTokens += msgTokens;
            } else {
                break;
            }
        }

        Collections.reverse(retained); 
        return retained;
    }

    public List<AiMessage> truncateHistoryAi(
            List<AiMessage> history,
            String systemPrompt,
            String currentQuestion,
            String retrievedKnowledge,
            int maxTotalTokens,
            int maxHistoryTokens) {

        int sysTokens = estimateTokens(systemPrompt);
        int qTokens = estimateTokens(currentQuestion);
        int ragTokens = estimateTokens(retrievedKnowledge);

        int availableTokensForHistory = maxTotalTokens - (sysTokens + qTokens + ragTokens);
        int allowedHistoryTokens = Math.min(availableTokensForHistory, maxHistoryTokens);

        if (allowedHistoryTokens <= 0 || history.isEmpty()) {
            return List.of();
        }

        List<AiMessage> retained = new ArrayList<>();
        int currentHistoryTokens = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            AiMessage msg = history.get(i);
            int msgTokens = estimateTokens(msg.content()) + 10;

            if (currentHistoryTokens + msgTokens <= allowedHistoryTokens) {
                retained.add(msg);
                currentHistoryTokens += msgTokens;
            } else {
                break;
            }
        }

        Collections.reverse(retained);
        return retained;
    }
}
