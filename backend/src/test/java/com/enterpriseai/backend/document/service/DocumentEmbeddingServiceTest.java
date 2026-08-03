package com.enterpriseai.backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.enterpriseai.backend.ai.config.EmbeddingProperties;
import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.DocumentType;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;

class DocumentEmbeddingServiceTest {

    private EmbeddingProvider embeddingProvider;
    private VectorStore vectorStore;
    private DocumentEmbeddingService service;
    private KnowledgeDocument document;
    private List<DocumentChunk> chunks;

    @BeforeEach
    void setUp() {
        embeddingProvider = org.mockito.Mockito.mock(EmbeddingProvider.class);
        vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        service = new DocumentEmbeddingService(
                embeddingProvider,
                vectorStore,
                new EmbeddingProperties("test", 2));

        User user = new User();
        user.setId(7L);
        user.setRole(Role.USER);

        com.enterpriseai.backend.workspace.entity.Workspace workspace = new com.enterpriseai.backend.workspace.entity.Workspace();
        workspace.setId(7L);

        document = new KnowledgeDocument();
        document.setId(4L);
        document.setCreatedBy(user);
        document.setWorkspace(workspace);
        document.setOriginalFileName("policy.txt");
        document.setDocumentType(DocumentType.TXT);

        chunks = List.of(chunk(11L, 0, "first"), chunk(12L, 1, "second"), chunk(13L, 2, "third"));
    }

    @Test
    void embedsInConfiguredBatchesAndStoresMetadata() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)));
        when(embeddingProvider.embedBatch(List.of("third")))
                .thenReturn(List.of(List.of(5.0, 6.0)));

        service.embedAndStore(document, chunks);

        verify(vectorStore).upsertBatch(
                List.of(11L, 12L),
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)),
                List.of(
                        Map.of("documentId", 4L, "chunkId", 11L, "workspaceId", 7L, "createdById", 7L,
                                "chunkIndex", 0, "originalFileName", "policy.txt", "documentType", "TXT", "chunkText", "first"),
                        Map.of("documentId", 4L, "chunkId", 12L, "workspaceId", 7L, "createdById", 7L,
                                "chunkIndex", 1, "originalFileName", "policy.txt", "documentType", "TXT", "chunkText", "second")));
        verify(vectorStore).upsertBatch(
                List.of(13L),
                List.of(List.of(5.0, 6.0)),
                List.of(Map.of("documentId", 4L, "chunkId", 13L, "workspaceId", 7L, "createdById", 7L,
                        "chunkIndex", 2, "originalFileName", "policy.txt", "documentType", "TXT", "chunkText", "third")));
    }

    @Test
    void rejectsEmbeddingCountMismatchAndCleansVectors() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, 2.0)));

        AiEmbeddingException exception = assertThrows(
                AiEmbeddingException.class,
                () -> service.embedAndStore(document, chunks));

        assertEquals("AI provider returned an embedding count mismatch", exception.getMessage());
        verify(vectorStore).deleteByDocument(4L);
    }

    @Test
    void rejectsInconsistentDimensionsAndCleansVectors() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, 2.0), List.of(3.0)));

        assertThrows(AiEmbeddingException.class, () -> service.embedAndStore(document, chunks));

        verify(vectorStore).deleteByDocument(4L);
    }

    @Test
    void rejectsNonFiniteValuesAndCleansVectors() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, Double.NaN), List.of(3.0, 4.0)));

        assertThrows(AiEmbeddingException.class, () -> service.embedAndStore(document, chunks));

        verify(vectorStore).deleteByDocument(4L);
    }

    @Test
    void providerFailureCleansAllVectors() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)));
        when(embeddingProvider.embedBatch(List.of("third")))
                .thenThrow(new RuntimeException("provider unavailable"));

        assertThrows(AiEmbeddingException.class, () -> service.embedAndStore(document, chunks));

        verify(vectorStore).deleteByDocument(4L);
    }

    @Test
    void vectorStoreFailureCleansAllVectors() {
        when(embeddingProvider.embedBatch(List.of("first", "second")))
                .thenReturn(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)));
        doThrow(new RuntimeException("qdrant unavailable"))
                .when(vectorStore).upsertBatch(anyList(), anyList(), anyList());

        assertThrows(AiEmbeddingException.class, () -> service.embedAndStore(document, chunks));

        verify(vectorStore).deleteByDocument(4L);
    }

    @Test
    void deleteVectorsDelegatesByDocumentId() {
        service.deleteVectors(document);

        verify(vectorStore).deleteByDocument(4L);
        verifyNoInteractions(embeddingProvider);
    }

    private DocumentChunk chunk(Long id, int index, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setDocument(document);
        return chunk;
    }
}
