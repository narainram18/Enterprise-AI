package com.enterpriseai.backend.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.retrieval.hybrid.HybridSearchService;
import com.enterpriseai.backend.ai.retrieval.hybrid.KeywordSearchService;
import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.retrieval.reranker.CrossEncoderReranker;
import com.enterpriseai.backend.ai.retrieval.reranker.FallbackReranker;
import com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService;
import com.enterpriseai.backend.ai.retrieval.service.QueryEmbeddingService;
import com.enterpriseai.backend.ai.retrieval.service.RetrievalContextBuilder;
import com.enterpriseai.backend.ai.retrieval.service.SemanticSearchService;
import com.enterpriseai.backend.ai.tool.ToolExecutor;
import com.enterpriseai.backend.ai.tool.ToolRegistry;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import com.enterpriseai.backend.entity.DocumentType;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RagRegressionIntegrationTest {

    private ChatRetrievalService chatRetrievalService;
    private VectorStore vectorStore;
    private KeywordSearchService keywordSearchService;
    
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        vectorStore = Mockito.mock(VectorStore.class);
        keywordSearchService = Mockito.mock(KeywordSearchService.class);
        
        EmbeddingProvider embeddingProvider = Mockito.mock(EmbeddingProvider.class);
        when(embeddingProvider.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        
        RetrievalProperties properties = new RetrievalProperties(
                true, 20, 0.4, 5, 5, 12000, 5000, true, true, "STRICT");
                
        QueryEmbeddingService queryEmbeddingService = new QueryEmbeddingService(embeddingProvider);
        SemanticSearchService semanticSearchService = new SemanticSearchService(
                queryEmbeddingService, vectorStore, properties, new SimpleMeterRegistry());
                
        CrossEncoderReranker reranker = new FallbackReranker();
        RetrievalPipeline pipeline = new HybridSearchService(semanticSearchService, keywordSearchService, reranker);
        
        RetrievalContextBuilder contextBuilder = new RetrievalContextBuilder(properties);
        
        chatRetrievalService = new ChatRetrievalService(
                Mockito.mock(UserRepository.class), pipeline, contextBuilder, properties);

        WorkspaceContext context = new WorkspaceContext(1L, com.enterpriseai.backend.workspace.entity.WorkspaceRole.OWNER, false);
        WorkspaceContextHolder.setContext(context);
    }
    
    @Test
    void duplicateDocumentsAreDeduplicatedBeforeFinalTopK() {
        // Setup semantic search to return multiple duplicate copies of the same chunks (as if uploaded 10 times)
        // Chunk 0 is the title, Chunk 1 contains the CEO info.
        
        // Return 10 copies of Chunk 0, 10 copies of Chunk 1
        List<VectorSearchResult> semanticResults = new java.util.ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            semanticResults.add(new VectorSearchResult(i * 100, 0.85, Map.of(
                    "workspaceId", 1L, "documentId", i, "chunkId", i * 100, "chunkIndex", 0,
                    "originalFileName", "Employee_Handbook.txt", "documentType", "TXT",
                    "chunkText", "Employee Handbook Title Page"
            )));
            semanticResults.add(new VectorSearchResult(i * 100 + 1, 0.95, Map.of(
                    "workspaceId", 1L, "documentId", i, "chunkId", i * 100 + 1, "chunkIndex", 1,
                    "originalFileName", "Employee_Handbook.txt", "documentType", "TXT",
                    "chunkText", "The CEO of the company is John Doe."
            )));
        }
        
        when(vectorStore.search(anyList(), eq(20), eq(1L))).thenReturn(semanticResults);
        when(keywordSearchService.search(anyString(), eq(1L))).thenReturn(List.of());

        ChatRetrievalResult result = chatRetrievalService.retrieve("Who is the CEO?", "user@test.com");
        
        String context = result.context();
        
        // Assert that the context contains BOTH chunk 0 and chunk 1.
        // If deduplication before Top-K works, we should get exactly 1 copy of Chunk 0 and 1 copy of Chunk 1,
        // rather than 5 copies of Chunk 1 and NO copies of Chunk 0, or vice-versa.
        assertTrue(context.contains("Employee Handbook Title Page"), "Context should contain the title page");
        assertTrue(context.contains("The CEO of the company is John Doe."), "Context should contain the CEO info");
        
        // Assert that it's actually deduplicated
        int titleCount = org.springframework.util.StringUtils.countOccurrencesOf(context, "Employee Handbook Title Page");
        int ceoCount = org.springframework.util.StringUtils.countOccurrencesOf(context, "The CEO of the company is John Doe.");
        
        assertEquals(1, titleCount, "Should only have 1 copy of title page");
        assertEquals(1, ceoCount, "Should only have 1 copy of CEO info");
    }
}
