package com.enterpriseai.backend.document.chunking;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.KnowledgeDocument;

@Service
public class TextChunkingService {

    private final DocumentChunkingProperties properties;

    public TextChunkingService(DocumentChunkingProperties properties) {
        this.properties = properties;
    }

    public List<DocumentChunk> createChunks(KnowledgeDocument document, String text) {
        if (properties.getChunkOverlap() >= properties.getChunkSize()) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int chunkSize = properties.getChunkSize();
        int overlap = properties.getChunkOverlap();
        int textLength = text.length();
        int currentIndex = 0;
        int chunkIndex = 0;

        while (currentIndex < textLength) {
            int endIndex = Math.min(currentIndex + chunkSize, textLength);
            
            if (endIndex < textLength) {
                int boundary = findBoundary(text, currentIndex, endIndex);
                if (boundary > currentIndex) {
                    endIndex = boundary;
                }
            }

            String chunkContent = text.substring(currentIndex, endIndex).trim();
            if (!chunkContent.isEmpty()) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(document);
                chunk.setChunkIndex(chunkIndex++);
                chunk.setContent(chunkContent);
                chunk.setCharacterCount(chunkContent.length());
                chunks.add(chunk);
            }

            if (endIndex >= textLength) {
                break;
            }

            int nextStart = endIndex - overlap;
            if (nextStart <= currentIndex) {
                nextStart = endIndex;
            }
            currentIndex = nextStart;
        }
        
        return chunks;
    }

    private int findBoundary(String text, int start, int end) {
        int minBoundary = start + (int) ((end - start) * 0.5);

        int[] boundaries = {
            text.lastIndexOf("\n\n", Math.max(start, end - 2)),
            text.lastIndexOf("\n", Math.max(start, end - 1)),
            text.lastIndexOf(". ", Math.max(start, end - 2)),
            text.lastIndexOf(" ", Math.max(start, end - 1))
        };
        
        int[] offsets = {2, 1, 2, 1};

        for (int i = 0; i < boundaries.length; i++) {
            if (boundaries[i] >= minBoundary) {
                return boundaries[i] + offsets[i];
            }
        }

        for (int i = 0; i < boundaries.length; i++) {
            if (boundaries[i] > start) {
                return boundaries[i] + offsets[i];
            }
        }

        return end;
    }
}
