package com.enterpriseai.backend.ai.retrieval.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;

@Component
public class RetrievalContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(RetrievalContextBuilder.class);

    private final RetrievalProperties properties;

    public RetrievalContextBuilder(RetrievalProperties properties) {
        this.properties = properties;
    }

    public String build(List<RetrievedChunk> chunks) {
        log.info("RetrievalContextBuilder: CALLED");
        List<RetrievedChunk> selected = select(chunks);
        if (selected.isEmpty()) {
            return "";
        }

        // Group by Document
        Map<String, List<RetrievedChunk>> byDocument = new LinkedHashMap<>();
        for (RetrievedChunk chunk : selected) {
            byDocument.computeIfAbsent(chunk.documentFileName(), k -> new ArrayList<>()).add(chunk);
        }

        StringBuilder context = new StringBuilder();
        
        for (Map.Entry<String, List<RetrievedChunk>> entry : byDocument.entrySet()) {
            String fileName = entry.getKey();
            List<RetrievedChunk> docChunks = entry.getValue();
            
            // Sort by chunk index to merge contiguous chunks
            docChunks.sort(Comparator.comparingInt(RetrievedChunk::chunkIndex));
            
            context.append("[Document: ").append(fileName).append("]\n\n");
            
            for (RetrievedChunk chunk : docChunks) {
                String formatted = format(chunk, properties.citationsEnabled());
                int separatorLength = context.length() == 0 ? 0 : 2;
                int remaining = properties.maximumRetrievedCharacters() - context.length() - separatorLength;
                
                if (remaining <= 0) break;
                
                if (formatted.length() > remaining) {
                    formatted = formatted.substring(0, remaining).stripTrailing();
                }
                
                context.append(formatted).append("\n\n");
            }
        }
        
        String finalContext = context.toString().trim();
        log.info("Generated retrieval context:\n{}", finalContext);
        return finalContext;
    }

    public List<RetrievedChunk> select(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, RetrievedChunk> unique = new LinkedHashMap<>();
        Set<Long> documentIds = new LinkedHashSet<>();
        
        chunks.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingDouble(RetrievedChunk::similarityScore).reversed()
                        .thenComparing(RetrievedChunk::documentId)
                        .thenComparing(RetrievedChunk::chunkIndex))
                .forEach(chunk -> {
                    if (unique.containsKey(chunk.chunkId())) {
                        return;
                    }
                    if (!documentIds.contains(chunk.documentId())
                            && documentIds.size() >= properties.maximumRetrievedDocuments()) {
                        return;
                    }
                    
                    if (properties.contextCompressionEnabled()) {
                        boolean duplicate = unique.values().stream()
                            .anyMatch(existing -> existing.chunkText().trim().equalsIgnoreCase(chunk.chunkText().trim()));
                        if (duplicate) return;
                    }
                    
                    documentIds.add(chunk.documentId());
                    unique.put(chunk.chunkId(), chunk);
                });

        return unique.values().stream()
                .limit(properties.maximumRetrievedChunks())
                .toList();
    }

    private String format(RetrievedChunk chunk, boolean enableCitations) {
        if (enableCitations) {
            return "--- Section " + chunk.chunkIndex() + " ---\n" + chunk.chunkText();
        }
        return "Section: " + chunk.chunkIndex() + "\n\nContent:\n" + chunk.chunkText();
    }
}
