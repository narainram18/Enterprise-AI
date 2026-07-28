package com.enterpriseai.backend.ai.context;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.model.AiChatRequest;

@Component
public class DefaultChatContextExtension implements ChatContextExtension {

    private final RagPromptBuilder promptBuilder;

    public DefaultChatContextExtension(RagPromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    @Override
    public AiChatRequest extend(AiChatRequest historyRequest, String retrievalContext) {
        return promptBuilder.build(historyRequest, retrievalContext);
    }
}
