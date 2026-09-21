package com.enterpriseai.backend.ai.evaluation;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.retrieval.service.RetrievalContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RagEvaluationTest {

    private RetrievalContextBuilder contextBuilder;
    private RetrievalProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RetrievalProperties(
                true, // enabled
                10,   // topK
                0.5,  // minimumSimilarityScore
                5,    // maximumRetrievedChunks
                3,    // maximumRetrievedDocuments
                4000, // maximumRetrievedCharacters
                5000L, // searchTimeout
                true, // contextCompressionEnabled
                true,  // citationsEnabled
                "STRICT" // groundingMode
        );
        contextBuilder = new RetrievalContextBuilder(properties);
    }

    @Test
    void testRetrievalSuccessAndSourceRelevance() {
        // Evaluate that valid chunks from multiple documents are selected correctly
        RetrievedChunk c1 = new RetrievedChunk(1L, 100L, 1, 5, 0.95, "Annual_Report_2025.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "Revenue in 2025 was $50M.", 1L);
        RetrievedChunk c2 = new RetrievedChunk(2L, 101L, 1, 2, 0.85, "Q1_Summary.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "Q1 saw a significant increase.", 1L);

        List<RetrievedChunk> selected = contextBuilder.select(List.of(c1, c2));

        assertEquals(2, selected.size());
        assertEquals("Annual_Report_2025.pdf", selected.get(0).documentFileName());
    }

    @Test
    void testNegativeRetrievalRejectsUnrelated() {
        // Evaluate that duplicate or unrelated chunks (low score) would be handled
        // Note: the pipeline itself filters by threshold, but here we test context builder compression
        RetrievedChunk c1 = new RetrievedChunk(1L, 100L, 1, 1, 0.9, "DocA.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "Duplicate text here.", 1L);
        RetrievedChunk c2 = new RetrievedChunk(1L, 100L, 2, 2, 0.88, "DocA.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "Duplicate text here.", 1L);
        RetrievedChunk c3 = new RetrievedChunk(2L, 101L, 1, 1, 0.8, "DocB.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "Unique text.", 1L);

        List<RetrievedChunk> selected = contextBuilder.select(List.of(c1, c2, c3));

        // Compression should drop c2 because it's a duplicate of c1
        assertEquals(2, selected.size());
        assertEquals(100L, selected.get(0).chunkId());
        assertEquals(101L, selected.get(1).chunkId());
    }

    @Test
    void testCitationAndSourceCorrectness() {
        // Evaluate that context is built with proper [Workspace Source: ...] headers
        RetrievedChunk c1 = new RetrievedChunk(1L, 100L, 1, 1, 0.9, "Strategy_2025.pdf", com.enterpriseai.backend.entity.DocumentType.PDF, "AI strategy focuses on RAG.", 1L);

        String context = contextBuilder.build(List.of(c1));

        assertTrue(context.contains("[Workspace Source: Strategy_2025.pdf]"));
        assertTrue(context.contains("--- Section 1 ---"));
        assertTrue(context.contains("AI strategy focuses on RAG."));
    }
}
