package com.enterpriseai.backend.ai.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;

class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder();

    @Test
    void addsProviderNeutralInstructionsAndRetrievedKnowledge() {
        AiChatRequest history = new AiChatRequest(List.of(
                new AiMessage(AiMessageRole.USER, "What are my responsibilities?")));

        AiChatRequest result = builder.build(history, "[Document: handbook.pdf]\n\nContent:\nOwn onboarding");

        assertEquals(AiMessageRole.SYSTEM, result.messages().getFirst().role());
        assertTrue(result.messages().getFirst().content().contains("Retrieved Knowledge"));
        assertTrue(result.messages().getFirst().content().contains("Own onboarding"));
        assertEquals(history.messages(), result.messages().subList(1, result.messages().size()));
    }

    @Test
    void leavesPromptUnchangedWhenThereIsNoRetrievedKnowledge() {
        AiChatRequest history = new AiChatRequest(List.of(new AiMessage(AiMessageRole.USER, "Hello")));

        assertEquals(history, builder.build(history, ""));
    }
}
