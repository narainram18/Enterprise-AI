package com.enterpriseai.backend.ai;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.retrieval.service.RetrievalContextBuilder;
import com.enterpriseai.backend.entity.DocumentType;

public class RagArchitectureTest {

    @Test
    void testTokenBudgetManagerTruncation() {
        TokenBudgetManager manager = new TokenBudgetManager();
        
        List<AiMessage> history = List.of(
            new AiMessage(AiMessageRole.USER, "Oldest question"),
            new AiMessage(AiMessageRole.ASSISTANT, "Oldest answer"),
            new AiMessage(AiMessageRole.USER, "Recent question"),
            new AiMessage(AiMessageRole.ASSISTANT, "Recent answer")
        );

        // Very small budget to force truncation of the oldest messages
        int maxTotalTokens = 100;
        int maxHistoryTokens = 15; // Enough for roughly one or two messages
        
        List<AiMessage> truncated = manager.truncateHistoryAi(
            history,
            "system prompt",
            "current question",
            "retrieved knowledge",
            maxTotalTokens,
            maxHistoryTokens
        );

        assertNotNull(truncated);
        assertTrue(truncated.size() < history.size(), "History should be truncated");
        
        // Ensure the most recent message is kept
        assertEquals("Recent answer", truncated.get(truncated.size() - 1).content());
    }

    @Test
    void testRagPromptBuilderFormatting() {
        TokenBudgetManager budgetManager = new TokenBudgetManager();
        AiChatProperties chatProps = new AiChatProperties(10, 5000, 1000, 2000, 4000);
        RagPromptBuilder builder = new RagPromptBuilder(budgetManager, chatProps);

        AiChatRequest historyRequest = new AiChatRequest(List.of(
            new AiMessage(AiMessageRole.USER, "What is the policy?"),
            new AiMessage(AiMessageRole.ASSISTANT, "The policy is X.")
        ), null, null, null);

        String retrievalContext = "[Document: policy.pdf]\n\n--- Section 1 ---\nPolicy is Y.";

        AiChatRequest result = builder.build(
            new Agent("test-agent", "Test", "Desc", "Icon", "Color", "You are a test agent", 0.7, 0.9, "Model", true, true, java.util.List.of(), null, null, java.util.List.of()),
            historyRequest, 
            retrievalContext
        );

        assertEquals(2, result.messages().size(), "Result should exactly have SYSTEM and USER messages");
        assertEquals(AiMessageRole.SYSTEM, result.messages().get(0).role());
        assertEquals(AiMessageRole.USER, result.messages().get(1).role());

        String userContent = result.messages().get(1).content();
        assertTrue(userContent.contains("Conversation History"), "Must contain history");
        assertTrue(userContent.contains("What is the policy?"), "Must contain past user question");
        assertTrue(userContent.contains("Retrieved Knowledge"), "Must contain retrieval context block");
        assertTrue(userContent.contains("Policy is Y."), "Must contain actual retrieved text");
        assertTrue(userContent.contains("Current Question"), "Must contain current question block");
    }

    @Test
    void testRetrievalContextBuilderGroupingAndCompression() {
        RetrievalProperties props = new RetrievalProperties(true, 10, 0.5, 5, 5, 10000, 5000, true, true, "STRICT");
        RetrievalContextBuilder builder = new RetrievalContextBuilder(props);

        List<RetrievedChunk> chunks = List.of(
            new RetrievedChunk(1L, 101L, 2,null, 0.9, "docA.pdf", DocumentType.PDF, "Content A part 2", 1L),
            new RetrievedChunk(1L, 100L, 1,null, 0.85, "docA.pdf", DocumentType.PDF, "Content A part 1", 1L),
            new RetrievedChunk(2L, 201L, 1,null, 0.8, "docB.pdf", DocumentType.PDF, "Content B part 1", 1L),
            // Duplicate chunk text to test compression
            new RetrievedChunk(2L, 202L, 2,null, 0.75, "docB.pdf", DocumentType.PDF, "Content B part 1", 1L) 
        );

        String context = builder.build(chunks);

        // Document grouping check
        assertTrue(context.indexOf("[Document: docA.pdf]") < context.indexOf("[Document: docB.pdf]"));
        
        // Chunk ordering check (Section 1 should appear before Section 2 despite lower similarity)
        assertTrue(context.indexOf("Content A part 1") < context.indexOf("Content A part 2"));
        
        System.out.println("DEBUG CONTEXT:\n" + context);
        // Compression check (Duplicate text should be removed)
        int bCount = context.split("Content B part 1", -1).length - 1;
        assertEquals(1, bCount, "Duplicate chunks should be compressed/removed");
    }
}

