package com.enterpriseai.backend.document.chunking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.KnowledgeDocument;

class TextChunkingServiceTest {

    private TextChunkingService service;
    private DocumentChunkingProperties properties;
    private KnowledgeDocument document;

    @BeforeEach
    void setUp() {
        properties = new DocumentChunkingProperties();
        properties.setChunkSize(20);
        properties.setChunkOverlap(5);
        service = new TextChunkingService(properties);
        
        document = new KnowledgeDocument();
        document.setId(1L);
    }

    @Test
    void createChunks_withShortText_createsOneChunk() {
        String text = "Short text.";
        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("Short text.");
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).getCharacterCount()).isEqualTo(11);
    }

    @Test
    void createChunks_withLongText_createsMultipleChunks() {
        // 44 characters total
        String text = "12345678901234567890123456789012345678901234";
        
        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks.size()).isGreaterThan(1);
        
        // Assert sequence
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
            assertThat(chunks.get(i).getDocument()).isEqualTo(document);
        }
    }

    @Test
    void createChunks_respectsParagraphBoundaries() {
        properties.setChunkSize(30);
        properties.setChunkOverlap(5);
        // Paragraph boundary at index 15
        String text = "First paragraph\n\nSecond paragraph here";
        
        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("First paragraph");
        assertThat(chunks.get(1).getContent()).isEqualTo("aph\n\nSecond paragraph here");
    }

    @Test
    void createChunks_rejectsInvalidConfiguration() {
        properties.setChunkSize(10);
        properties.setChunkOverlap(10); // overlap >= size is invalid

        assertThatThrownBy(() -> service.createChunks(document, "Some text"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createChunks_ignoresEmptyOrNullText() {
        assertThat(service.createChunks(document, null)).isEmpty();
        assertThat(service.createChunks(document, "   ")).isEmpty();
    }

    @Test
    void createChunks_noEmptyChunks() {
        properties.setChunkSize(10);
        properties.setChunkOverlap(2);
        
        String text = "A        B"; // Lots of spaces
        List<DocumentChunk> chunks = service.createChunks(document, text);
        
        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.getContent()).isNotBlank();
        }
    }
}
