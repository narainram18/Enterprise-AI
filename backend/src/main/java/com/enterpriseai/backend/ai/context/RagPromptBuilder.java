package com.enterpriseai.backend.ai.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;

@Component
public class RagPromptBuilder {

    private static final String INSTRUCTIONS = "You are an enterprise assistant. "
            + "Use retrieved information when relevant. If it is insufficient, answer using general knowledge. "
            + "Never fabricate document contents. Prefer retrieved information over assumptions and mention uncertainty when appropriate.";

    public AiChatRequest build(AiChatRequest historyRequest, String retrievalContext) {
        if (retrievalContext == null || retrievalContext.isBlank()) {
            return historyRequest;
        }

        String systemPrompt = INSTRUCTIONS
                + "\n\n====================\nRetrieved Knowledge\n====================\n\n"
                + retrievalContext
                + "\n\n====================\nConversation History\n====================\n\n"
                + "The conversation messages that follow are the conversation history."
                + "\n\n====================\nUser Question\n====================\n\n"
                + "The latest user message is the question to answer.";
        List<AiMessage> messages = new ArrayList<>();
        messages.add(new AiMessage(AiMessageRole.SYSTEM, systemPrompt));
        messages.addAll(historyRequest.messages());
        return new AiChatRequest(List.copyOf(messages));
    }
}
