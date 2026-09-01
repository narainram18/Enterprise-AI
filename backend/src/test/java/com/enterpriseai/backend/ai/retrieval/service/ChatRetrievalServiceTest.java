package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.entity.WorkspaceRole;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.DocumentType;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChatRetrievalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RetrievalPipeline retrievalPipeline;

    @Mock
    private RetrievalContextBuilder contextBuilder;

    private ChatRetrievalService service;

    @BeforeEach
    void setUp() {
        service = new ChatRetrievalService(
                userRepository,
                retrievalPipeline,
                contextBuilder,
                new RetrievalProperties(true, 5, 0.5, 5, 3, 1000, 1000, true, true, "STRICT"));
        WorkspaceContextHolder.setContext(new WorkspaceContext(7L, WorkspaceRole.OWNER, true));
    }

    @Test
    void returnsContextCitationsAndStatisticsForRetrievedChunks() {
        RetrievedChunk chunk = new RetrievedChunk(
                11L, 22L, 1, null, 0.91, "handbook.pdf", DocumentType.PDF, "Responsibilities", 7L);

        when(retrievalPipeline.retrieveAndRank("responsibilities", 7L)).thenReturn(List.of(chunk));
        when(contextBuilder.select(List.of(chunk))).thenReturn(List.of(chunk));
        when(contextBuilder.build(List.of(chunk))).thenReturn("Responsibilities");

        var result = service.retrieve("responsibilities", "user@example.com");

        assertEquals("Responsibilities", result.context());
        assertEquals(1, result.citations().size());
        assertEquals(11L, result.citations().getFirst().documentId());
        assertEquals(1, result.statistics().retrievedChunks());
        assertEquals(1, result.statistics().retrievedDocuments());
        assertTrue(result.statistics().retrievalAttempted());
    }

    @Test
    void retrievalFailureDoesNotEscapeToChat() {
        when(retrievalPipeline.retrieveAndRank(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Qdrant unavailable"));

        var result = service.retrieve("question", "user@example.com");

        assertEquals("", result.context());
        assertTrue(result.citations().isEmpty());
        assertTrue(result.statistics().retrievalAttempted());
    }

    @Test
    void disabledRetrievalDoesNotResolveUserOrSearch() {
        service = new ChatRetrievalService(
                userRepository,
                retrievalPipeline,
                contextBuilder,
                new RetrievalProperties(false, 5, 0.5, 5, 3, 1000, 1000, true, true, "STRICT"));

        var result = service.retrieve("question", "user@example.com");

        assertTrue(result.context().isEmpty());
        assertTrue(!result.statistics().retrievalAttempted());
    }
}
