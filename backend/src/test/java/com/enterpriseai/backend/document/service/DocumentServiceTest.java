package com.enterpriseai.backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import com.enterpriseai.backend.document.config.DocumentUploadProperties;
import com.enterpriseai.backend.document.extractor.TextExtractionException;
import com.enterpriseai.backend.document.extractor.TextExtractionService;
import com.enterpriseai.backend.document.storage.FileStorageService;
import com.enterpriseai.backend.dto.DocumentResponse;
import com.enterpriseai.backend.dto.DocumentDetailsResponse;
import org.springframework.data.jpa.domain.Specification;
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.entity.DocumentChunk;
import com.enterpriseai.backend.entity.DocumentProcessingStatus;
import com.enterpriseai.backend.entity.DocumentType;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.BadRequestException;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.repository.DocumentChunkRepository;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.document.chunking.TextChunkingService;

class DocumentServiceTest {

    private UserRepository userRepository;
    private KnowledgeDocumentRepository documentRepository;
    private FileStorageService fileStorageService;
    private TextExtractionService textExtractionService;
    private TextChunkingService textChunkingService;
    private DocumentChunkRepository documentChunkRepository;
    private DocumentEmbeddingService documentEmbeddingService;
    private com.enterpriseai.backend.workspace.repository.WorkspaceRepository workspaceRepository;
    private DocumentService documentService;
    private User user;
    private com.enterpriseai.backend.workspace.entity.Workspace workspace;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        documentRepository = org.mockito.Mockito.mock(KnowledgeDocumentRepository.class);
        fileStorageService = org.mockito.Mockito.mock(FileStorageService.class);
        textExtractionService = org.mockito.Mockito.mock(TextExtractionService.class);
        textChunkingService = org.mockito.Mockito.mock(TextChunkingService.class);
        documentChunkRepository = org.mockito.Mockito.mock(DocumentChunkRepository.class);
        documentEmbeddingService = org.mockito.Mockito.mock(DocumentEmbeddingService.class);
        workspaceRepository = org.mockito.Mockito.mock(com.enterpriseai.backend.workspace.repository.WorkspaceRepository.class);
        documentService = new DocumentService(
                userRepository, documentRepository, fileStorageService, textExtractionService,
                new DocumentUploadProperties(10_485_760), textChunkingService, documentChunkRepository,
                documentEmbeddingService, workspaceRepository, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        user = new User();
        user.setId(7L);
        user.setEmail("owner@example.com");
        user.setName("Owner");
        user.setRole(Role.USER);

        workspace = new com.enterpriseai.backend.workspace.entity.Workspace();
        workspace.setId(100L);

        com.enterpriseai.backend.workspace.context.WorkspaceContextHolder.setContext(
                new com.enterpriseai.backend.workspace.context.WorkspaceContext(100L, com.enterpriseai.backend.workspace.entity.WorkspaceRole.OWNER));
        
        when(workspaceRepository.getReferenceById(100L)).thenReturn(workspace);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> {
                    KnowledgeDocument document = invocation.getArgument(0);
                    if (document.getId() == null) {
                        document.setId(4L);
                    }
                    return document;
                });
        when(documentChunkRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Default: chunking produces no chunks (overridden per-test where needed)
        when(textChunkingService.createChunks(any(KnowledgeDocument.class), any()))
                .thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // 8. Successful upload persists extracted text AND chunks, marks READY
    // 9. Existing extracted text is preserved unchanged in the saved entity
    // -----------------------------------------------------------------------
    @Test
    void uploadsDocumentAndPersistsReadyText() throws Exception {
        when(fileStorageService.store(any())).thenReturn("trusted-key");
        when(fileStorageService.open("trusted-key"))
                .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any())).thenReturn("hello");

        DocumentDetailsResponse response = documentService.upload(
                "owner@example.com", file("notes.txt", "hello", "text/plain"));

        assertEquals(DocumentProcessingStatus.READY, response.processingStatus());
        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        KnowledgeDocument saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(DocumentProcessingStatus.READY, saved.getProcessingStatus());
        // 9. Extracted text is unchanged
        assertEquals("hello", saved.getExtractedText());
        assertNull(saved.getExtractionError());
    }

    @Test
    void upload_persistsChunksReturnedByChunkingService() throws Exception {
        DocumentChunk chunk0 = new DocumentChunk();
        chunk0.setChunkIndex(0);
        chunk0.setContent("hello");
        chunk0.setCharacterCount(5);

        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setChunkIndex(1);
        chunk1.setContent("world");
        chunk1.setCharacterCount(5);

        when(fileStorageService.store(any())).thenReturn("key");
        when(fileStorageService.open("key"))
                .thenReturn(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any())).thenReturn("hello world");
        when(textChunkingService.createChunks(any(KnowledgeDocument.class), eq("hello world")))
                .thenReturn(List.of(chunk0, chunk1));

        documentService.upload("owner@example.com", file("doc.txt", "hello world", "text/plain"));

        // Chunks from the chunking service must be saved
        verify(documentChunkRepository).saveAll(List.of(chunk0, chunk1));
    }

    // -----------------------------------------------------------------------
    // If chunking fails → FAILED status, file preserved, extracted text null
    // -----------------------------------------------------------------------
    @Test
    void upload_chunkingFailurePersistsFailedStatusAndKeepsFile() throws Exception {
        when(fileStorageService.store(any())).thenReturn("trusted-key");
        when(fileStorageService.open("trusted-key"))
                .thenReturn(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any())).thenReturn("content");
        when(textChunkingService.createChunks(any(), any()))
                .thenThrow(new RuntimeException("chunking failed"));

        DocumentDetailsResponse response = documentService.upload(
                "owner@example.com", file("doc.txt", "content", "text/plain"));

        assertEquals(DocumentProcessingStatus.FAILED, response.processingStatus());
        verify(documentChunkRepository).deleteByDocumentId(4L);
        verify(fileStorageService, never()).delete("trusted-key");
    }

    // -----------------------------------------------------------------------
    // Extraction failure → FAILED, file preserved (existing test)
    // -----------------------------------------------------------------------
    @Test
    void extractionFailurePersistsFailedStatusAndKeepsFile() throws Exception {
        when(fileStorageService.store(any())).thenReturn("trusted-key");
        when(fileStorageService.open("trusted-key"))
                .thenReturn(new ByteArrayInputStream("bad".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any()))
                .thenThrow(new TextExtractionException("No meaningful text found"));

        DocumentDetailsResponse response = documentService.upload(
                "owner@example.com", file("broken.txt", "bad", "text/plain"));

        assertEquals(DocumentProcessingStatus.FAILED, response.processingStatus());
        assertEquals("No meaningful text found", response.extractionError());
        verify(fileStorageService, never()).delete("trusted-key");
    }

    // -----------------------------------------------------------------------
    // 10. Document deletion triggers repository.delete() which cascades to chunks
    //     (ON DELETE CASCADE on FK in DB handles the actual row removal)
    // -----------------------------------------------------------------------
    @Test
    void deletesPhysicalFileAndOwnedDatabaseRecord() {
        KnowledgeDocument document = readyDocument();
        document.setStorageKey("trusted-key");
        when(documentRepository.findByIdAndWorkspaceId(4L, 100L)).thenReturn(Optional.of(document));

        documentService.delete("owner@example.com", 4L);

        // File is removed
        verify(fileStorageService).delete("trusted-key");
        // JPA delete triggers the DB cascade that removes associated chunks
        verify(documentRepository).delete(document);
    }

    // -----------------------------------------------------------------------
    // List, get, getText — existing tests preserved
    // -----------------------------------------------------------------------
    @Test
    void listsOnlyDocumentsForResolvedUser() {
        KnowledgeDocument document = readyDocument();
        when(documentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(document)));

        var page = documentService.list("owner@example.com", PageRequest.of(0, 10), null, null);

        assertEquals(1, page.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    void rejectsCrossUserDocumentAccess() {
        when(documentRepository.findByIdAndWorkspaceId(99L, 100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.get("owner@example.com", 99L));
    }

    @Test
    void returnsTextForOwnedReadyDocument() {
        KnowledgeDocument document = readyDocument();
        when(documentRepository.findByIdAndWorkspaceId(4L, 100L)).thenReturn(Optional.of(document));

        DocumentTextResponse response = documentService.getText("owner@example.com", 4L);

        // 9. Extracted text returned unchanged
        assertEquals("extracted", response.text());
    }

    @Test
    void rejectsEmptyAndUnsupportedFiles() {
        assertThrows(BadRequestException.class,
                () -> documentService.upload("owner@example.com",
                        file("empty.txt", "", "text/plain")));
        assertThrows(BadRequestException.class,
                () -> documentService.upload("owner@example.com",
                        file("script.exe", "x", "application/octet-stream")));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private KnowledgeDocument readyDocument() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(4L);
        document.setCreatedBy(user);
        document.setWorkspace(workspace);
        document.setOriginalFileName("notes.txt");
        document.setDocumentType(DocumentType.TXT);
        document.setProcessingStatus(DocumentProcessingStatus.READY);
        document.setExtractedText("extracted");
        return document;
    }

    private MockMultipartFile file(String name, String content, String contentType) {
        return new MockMultipartFile("file", name, contentType,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
