package com.enterpriseai.backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.entity.WorkspaceRole;

import com.enterpriseai.backend.ai.config.EmbeddingProperties;
import com.enterpriseai.backend.ai.provider.EmbeddingProvider;
import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import com.enterpriseai.backend.document.chunking.DocumentChunkingProperties;
import com.enterpriseai.backend.document.chunking.TextChunkingService;
import com.enterpriseai.backend.document.config.DocumentUploadProperties;
import com.enterpriseai.backend.document.extractor.PlainTextExtractor;
import com.enterpriseai.backend.document.extractor.TextExtractionService;
import com.enterpriseai.backend.document.storage.FileStorageService;
import com.enterpriseai.backend.dto.DocumentResponse;
import com.enterpriseai.backend.dto.DocumentDetailsResponse;
import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.DocumentChunkRepository;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.repository.UserRepository;

class DocumentIngestionPipelineIntegrationTest {

    private UserRepository userRepository;
    private KnowledgeDocumentRepository documentRepository;
    private FileStorageService fileStorageService;
    private DocumentChunkRepository documentChunkRepository;
    private InMemoryVectorStore vectorStore;
    private com.enterpriseai.backend.workspace.repository.WorkspaceRepository workspaceRepository;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        documentRepository = org.mockito.Mockito.mock(KnowledgeDocumentRepository.class);
        fileStorageService = org.mockito.Mockito.mock(FileStorageService.class);
        documentChunkRepository = org.mockito.Mockito.mock(DocumentChunkRepository.class);
        workspaceRepository = org.mockito.Mockito.mock(com.enterpriseai.backend.workspace.repository.WorkspaceRepository.class);

        User user = new User();
        user.setId(7L);
        user.setEmail("owner@example.com");
        user.setRole(Role.USER);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(java.util.Optional.of(user));

        com.enterpriseai.backend.workspace.entity.Workspace workspace = new com.enterpriseai.backend.workspace.entity.Workspace();
        workspace.setId(7L);
        when(workspaceRepository.getReferenceById(7L)).thenReturn(workspace);
        WorkspaceContextHolder.setContext(new WorkspaceContext(7L, WorkspaceRole.OWNER, true));

        when(documentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            document.setId(4L);
            return document;
        });
        when(documentChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            List<DocumentChunk> chunks = invocation.getArgument(0);
            long id = 10L;
            for (DocumentChunk chunk : chunks) {
                chunk.setId(id++);
            }
            return chunks;
        });

        String text = "First paragraph. Second paragraph. Third paragraph. Fourth paragraph.";
        when(fileStorageService.store(any())).thenReturn("storage-key");
        when(fileStorageService.open("storage-key"))
                .thenReturn(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));

        DocumentChunkingProperties chunkingProperties = new DocumentChunkingProperties();
        chunkingProperties.setChunkSize(25);
        chunkingProperties.setChunkOverlap(5);

        EmbeddingProvider embeddingProvider = new EmbeddingProvider() {
            @Override
            public List<Double> embed(String text) {
                return List.of(1.0, (double) text.length());
            }

            @Override
            public List<List<Double>> embedBatch(List<String> texts) {
                return texts.stream()
                        .map(value -> List.of(1.0, (double) value.length()))
                        .toList();
            }
        };
        vectorStore = new InMemoryVectorStore();
        DocumentEmbeddingService embeddingService = new DocumentEmbeddingService(
                embeddingProvider,
                vectorStore,
                new EmbeddingProperties("test", 2));

        documentService = new DocumentService(
                userRepository,
                documentRepository,
                fileStorageService,
                new TextExtractionService(List.of(new PlainTextExtractor())),
                new DocumentUploadProperties(10_485_760),
                new TextChunkingService(chunkingProperties),
                documentChunkRepository,
                embeddingService,
                workspaceRepository,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Test
    void uploadExtractsChunksEmbedsAndStoresVectorsBeforeReady() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "knowledge.txt", "text/plain", "ignored".getBytes(StandardCharsets.UTF_8));

        DocumentDetailsResponse response = documentService.upload("owner@example.com", file);

        assertEquals(com.enterpriseai.backend.entity.DocumentProcessingStatus.READY, response.processingStatus());
        assertFalse(vectorStore.points.isEmpty());
        assertEquals(4L, vectorStore.points.values().iterator().next().metadata().get("documentId"));
        assertEquals("knowledge.txt", vectorStore.points.values().iterator().next().metadata().get("originalFileName"));
    }

    private record Point(List<Double> vector, Map<String, Object> metadata) {
    }

    private static class InMemoryVectorStore implements VectorStore {
        private final Map<Long, Point> points = new HashMap<>();

        @Override
        public void upsert(Long chunkId, List<Double> embedding, Map<String, Object> metadata) {
            points.put(chunkId, new Point(embedding, metadata));
        }

        @Override
        public void upsertBatch(List<Long> chunkIds, List<List<Double>> embeddings,
                List<Map<String, Object>> metadatas) {
            for (int i = 0; i < chunkIds.size(); i++) {
                upsert(chunkIds.get(i), embeddings.get(i), metadatas.get(i));
            }
        }

        @Override
        public List<VectorSearchResult> search(List<Double> embedding, int topK, Long workspaceId) {
            return List.of();
        }

        @Override
        public void delete(Long chunkId) {
            points.remove(chunkId);
        }

        @Override
        public void deleteByDocument(Long documentId) {
            points.values().removeIf(point -> documentId.equals(point.metadata().get("documentId")));
        }
    }
}
