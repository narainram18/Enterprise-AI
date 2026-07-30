package com.enterpriseai.backend.ai.retrieval.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

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

        StringBuilder context = new StringBuilder();
        for (RetrievedChunk chunk : selected) {
            String formatted = format(chunk);
            int separatorLength = context.length() == 0 ? 0 : 2;
            int remaining = properties.maximumRetrievedCharacters() - context.length() - separatorLength;
            if (remaining <= 0) {
                break;
            }
            if (formatted.length() > remaining) {
                formatted = formatted.substring(0, remaining).stripTrailing();
            }
            if (context.length() > 0) {
                context.append("\n\n");
            }
            context.append(formatted);
        }
        log.info("Generated retrieval context:\n{}", context.toString());
        return context.toString();
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
                    documentIds.add(chunk.documentId());
                    unique.put(chunk.chunkId(), chunk);
                });

        return unique.values().stream()
                .limit(properties.maximumRetrievedChunks())
                .toList();
    }

    private String format(RetrievedChunk chunk) {
        return "[Document: " + chunk.documentFileName() + "]\n\n"
                + "Section: " + chunk.chunkIndex() + "\n\n"
                + "Content:\n" + chunk.chunkText();
    }
}
