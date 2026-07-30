package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.DocumentType;

class RetrievalContextBuilderTest {

    @Test
    void sortsDeduplicatesAndTruncatesContext() {
        RetrievalContextBuilder builder = new RetrievalContextBuilder(
                new RetrievalProperties(true, 10, 0.0, 2, 2, 1000, 1000, true, true, "STRICT"));

        String context = builder.build(List.of(
                chunk(12L, 4L, 1, 0.80, "second"),
                chunk(11L, 4L, 0, 0.95, "first"),
                chunk(11L, 4L, 0, 0.90, "duplicate"),
                chunk(13L, 4L, 2, 0.70, "truncated")));

        assertTrue(context.indexOf("first") < context.indexOf("second"));
        assertTrue(context.contains("[Document: handbook.txt]"));
        assertTrue(context.contains("--- Section 0 ---"));
        assertTrue(context.contains("first"));
        assertTrue(!context.contains("duplicate"));
        assertTrue(!context.contains("truncated"));
    }

    @Test
    void emptyResultsProduceEmptyContext() {
        RetrievalContextBuilder builder = new RetrievalContextBuilder(
                new RetrievalProperties(true, 10, 0.0, 2, 2, 1000, 1000, true, true, "STRICT"));

        assertEquals("", builder.build(List.of()));
    }

    @Test
    void respectsDocumentAndCharacterLimits() {
        RetrievalContextBuilder builder = new RetrievalContextBuilder(
                new RetrievalProperties(true, 10, 0.0, 10, 1, 80, 1000, true, true, "STRICT"));

        String context = builder.build(List.of(
                chunk(1L, 10L, 0, 0.9, "first"),
                chunk(2L, 20L, 0, 0.8, "second")));

        assertTrue(context.length() <= 80);
        assertTrue(context.contains("first"));
        assertTrue(!context.contains("second"));
    }

    private RetrievedChunk chunk(
            Long chunkId, Long documentId, int index, double score, String content) {
        return new RetrievedChunk(
                documentId, chunkId, index, score, "handbook.txt", DocumentType.TXT, content, 7L);
    }
}
