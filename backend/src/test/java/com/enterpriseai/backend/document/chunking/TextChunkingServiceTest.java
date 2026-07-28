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

    // -----------------------------------------------------------------------
    // 1. Short text creates exactly one chunk
    // -----------------------------------------------------------------------
    @Test
    void createChunks_withShortText_createsOneChunk() {
        String text = "Short text.";
        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("Short text.");
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).getCharacterCount()).isEqualTo(11);
    }

    // -----------------------------------------------------------------------
    // 2. Long text creates multiple chunks
    // 3. Chunk indexes are sequential from 0
    // -----------------------------------------------------------------------
    @Test
    void createChunks_withLongText_createsMultipleChunks() {
        // 44 characters total — well above chunkSize=20
        String text = "12345678901234567890123456789012345678901234";

        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks.size()).isGreaterThan(1);

        // Sequential indexes starting at 0, document reference correct
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
            assertThat(chunks.get(i).getDocument()).isEqualTo(document);
        }
    }

    // -----------------------------------------------------------------------
    // 4. No empty chunks
    // -----------------------------------------------------------------------
    @Test
    void createChunks_noEmptyChunks() {
        properties.setChunkSize(10);
        properties.setChunkOverlap(2);

        String text = "A        B"; // whitespace-heavy but non-blank
        List<DocumentChunk> chunks = service.createChunks(document, text);

        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.getContent()).isNotBlank();
            assertThat(chunk.getCharacterCount()).isGreaterThan(0);
        }
    }

    // -----------------------------------------------------------------------
    // 5. Overlap exists between consecutive chunks
    // -----------------------------------------------------------------------
    @Test
    void createChunks_overlapsExistBetweenConsecutiveChunks() {
        // chunkSize=20, overlap=5; use plain digits so no boundary is found
        // forcing a hard split — overlap will still come from the next-start calc
        properties.setChunkSize(20);
        properties.setChunkOverlap(5);
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefgh"; // 44 chars

        List<DocumentChunk> chunks = service.createChunks(document, text);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        // The tail of chunk N must appear at the start of chunk N+1
        for (int i = 0; i < chunks.size() - 1; i++) {
            String current = chunks.get(i).getContent();
            String next = chunks.get(i + 1).getContent();

            // The last `overlap` chars (or fewer for short chunks) of `current`
            // should be a prefix of `next`, verifying overlap
            int tailLength = Math.min(properties.getChunkOverlap(), current.length());
            String tail = current.substring(current.length() - tailLength);
            assertThat(next).startsWith(tail);
        }
    }

    // -----------------------------------------------------------------------
    // 6. Preferred text boundaries (paragraph, then newline, then sentence)
    // -----------------------------------------------------------------------
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
    void createChunks_respectsSentenceBoundary() {
        // Use a dedicated service so we don't depend on mutable setUp properties
        DocumentChunkingProperties props = new DocumentChunkingProperties();
        props.setChunkSize(20);
        props.setChunkOverlap(3);
        TextChunkingService svc = new TextChunkingService(props);

        // ". " boundary at index 11; chunkSize=20 → minBoundary=10; 11>=10 → primary branch fires
        // Expected split: "Hello world." (13 chars) then overlap from index 10 onward
        String text = "Hello world. Second sentence goes here.";

        List<DocumentChunk> chunks = svc.createChunks(document, text);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        // First chunk should end cleanly at the sentence boundary, not mid-word
        assertThat(chunks.get(0).getContent()).endsWith(".");
    }


    // -----------------------------------------------------------------------
    // 7. Invalid configuration is rejected
    // -----------------------------------------------------------------------
    @Test
    void createChunks_rejectsOverlapEqualToSize() {
        properties.setChunkSize(10);
        properties.setChunkOverlap(10); // overlap == size is invalid

        assertThatThrownBy(() -> service.createChunks(document, "Some text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void createChunks_rejectsOverlapGreaterThanSize() {
        properties.setChunkSize(10);
        properties.setChunkOverlap(15); // overlap > size is invalid

        assertThatThrownBy(() -> service.createChunks(document, "Some text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    // -----------------------------------------------------------------------
    // Boundary: empty / null / blank input produces zero chunks (not error)
    // -----------------------------------------------------------------------
    @Test
    void createChunks_ignoresEmptyOrNullText() {
        assertThat(service.createChunks(document, null)).isEmpty();
        assertThat(service.createChunks(document, "   ")).isEmpty();
        assertThat(service.createChunks(document, "")).isEmpty();
    }

    // -----------------------------------------------------------------------
    // characterCount matches trimmed content length
    // -----------------------------------------------------------------------
    @Test
    void createChunks_characterCountMatchesContentLength() {
        String text = "Hello world. This is a test of the chunking service.";
        properties.setChunkSize(20);
        properties.setChunkOverlap(3);

        List<DocumentChunk> chunks = service.createChunks(document, text);

        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.getCharacterCount()).isEqualTo(chunk.getContent().length());
        }
    }
}
