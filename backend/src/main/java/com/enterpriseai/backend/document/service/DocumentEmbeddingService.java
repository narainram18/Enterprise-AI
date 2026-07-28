package com.enterpriseai.backend.document.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.config.EmbeddingProperties;
import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.KnowledgeDocument;

@Service
public class DocumentEmbeddingService {

    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final EmbeddingProperties properties;

    public DocumentEmbeddingService(
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore,
            EmbeddingProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public void embedAndStore(KnowledgeDocument document, List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        if (document == null || document.getId() == null) {
            throw new AiEmbeddingException("Document must be persisted before embedding");
        }

        try {
            for (int start = 0; start < chunks.size(); start += properties.batchSize()) {
                int end = Math.min(start + properties.batchSize(), chunks.size());
                List<DocumentChunk> batch = chunks.subList(start, end);
                List<List<Double>> embeddings = embeddingProvider.embedBatch(
                        batch.stream().map(DocumentChunk::getContent).toList());
                validateEmbeddings(batch, embeddings);

                vectorStore.upsertBatch(
                        batch.stream().map(DocumentChunk::getId).toList(),
                        embeddings,
                        batch.stream().map(chunk -> metadata(document, chunk)).toList());
            }
        } catch (RuntimeException ex) {
            cleanup(document);
            if (ex instanceof AiEmbeddingException aiEmbeddingException) {
                throw aiEmbeddingException;
            }
            String rootMsg = getRootCauseMessage(ex);
            throw new AiEmbeddingException("Document embedding failed: " + rootMsg, ex);
        }
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    public void deleteVectors(KnowledgeDocument document) {
        if (document != null && document.getId() != null) {
            vectorStore.deleteByDocument(document.getId());
        }
    }

    private void validateEmbeddings(List<DocumentChunk> chunks, List<List<Double>> embeddings) {
        if (embeddings == null || embeddings.size() != chunks.size()) {
            throw new AiEmbeddingException("AI provider returned an embedding count mismatch");
        }

        int dimension = -1;
        for (List<Double> embedding : embeddings) {
            if (embedding == null || embedding.isEmpty()) {
                throw new AiEmbeddingException("AI provider returned an invalid vector");
            }
            if (dimension < 0) {
                dimension = embedding.size();
            } else if (embedding.size() != dimension) {
                throw new AiEmbeddingException("AI provider returned inconsistent vector dimensions");
            }
            if (embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
                throw new AiEmbeddingException("AI provider returned a vector with invalid values");
            }
        }
    }

    private Map<String, Object> metadata(KnowledgeDocument document, DocumentChunk chunk) {
        return Map.of(
                "documentId", document.getId(),
                "chunkId", chunk.getId(),
                "ownerId", document.getUser().getId(),
                "chunkIndex", chunk.getChunkIndex(),
                "originalFileName", document.getOriginalFileName(),
                "documentType", document.getDocumentType().name(),
                "chunkText", chunk.getContent());
    }

    private void cleanup(KnowledgeDocument document) {
        try {
            vectorStore.deleteByDocument(document.getId());
        } catch (RuntimeException cleanupFailure) {
            throw new AiEmbeddingException(
                    "Document embedding failed and vector cleanup failed", cleanupFailure);
        }
    }
}
