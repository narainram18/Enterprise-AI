package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;

class QueryEmbeddingServiceTest {

    private EmbeddingProvider provider;
    private QueryEmbeddingService service;

    @BeforeEach
    void setUp() {
        provider = org.mockito.Mockito.mock(EmbeddingProvider.class);
        service = new QueryEmbeddingService(provider);
    }

    @Test
    void delegatesQueryEmbeddingToProvider() {
        when(provider.embed("policy benefits")).thenReturn(List.of(1.0, 2.0));

        assertEquals(List.of(1.0, 2.0), service.embedQuery("policy benefits"));
        verify(provider).embed("policy benefits");
    }

    @Test
    void rejectsBlankQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.embedQuery("  "));
    }

    @Test
    void rejectsEmptyEmbedding() {
        when(provider.embed("query")).thenReturn(List.of());

        assertThrows(AiEmbeddingException.class, () -> service.embedQuery("query"));
    }

    @Test
    void rejectsNonFiniteEmbeddingValues() {
        when(provider.embed("query")).thenReturn(List.of(Double.POSITIVE_INFINITY));

        assertThrows(AiEmbeddingException.class, () -> service.embedQuery("query"));
    }
}
