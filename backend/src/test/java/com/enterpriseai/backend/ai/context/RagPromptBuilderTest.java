package com.enterpriseai.backend.ai.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;

import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.config.AiChatProperties;

class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder(
            new TokenBudgetManager(),
            new AiChatProperties(10, 1000, 1000, 2000, 4000)
    );

    @Test
    void addsProviderNeutralInstructionsAndRetrievedKnowledge() {
        AiChatRequest history = new AiChatRequest(List.of(
                new AiMessage(AiMessageRole.USER, "What are my responsibilities?")));

        AiChatRequest result = builder.build(history, "[Document: handbook.pdf]\n\nContent:\nOwn onboarding");

        assertEquals(AiMessageRole.SYSTEM, result.messages().getFirst().role());
        assertEquals(AiMessageRole.USER, result.messages().get(1).role());
        assertTrue(result.messages().get(1).content().contains("Retrieved Knowledge"));
        assertTrue(result.messages().get(1).content().contains("Own onboarding"));
        assertTrue(result.messages().get(1).content().contains("What are my responsibilities?"));
    }

    @Test
    void leavesPromptUnchangedWhenThereIsNoRetrievedKnowledge() {
        AiChatRequest history = new AiChatRequest(List.of(new AiMessage(AiMessageRole.USER, "Hello")));

        assertEquals(history, builder.build(history, ""));
    }
}
