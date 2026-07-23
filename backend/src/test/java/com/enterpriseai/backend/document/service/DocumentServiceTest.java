package com.enterpriseai.backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.entity.DocumentProcessingStatus;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.BadRequestException;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.repository.DocumentChunkRepository;
import com.enterpriseai.backend.document.chunking.TextChunkingService;

class DocumentServiceTest {

    private UserRepository userRepository;
    private KnowledgeDocumentRepository documentRepository;
    private FileStorageService fileStorageService;
    private TextExtractionService textExtractionService;
    private TextChunkingService textChunkingService;
    private DocumentChunkRepository documentChunkRepository;
    private DocumentService documentService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        documentRepository = org.mockito.Mockito.mock(KnowledgeDocumentRepository.class);
        fileStorageService = org.mockito.Mockito.mock(FileStorageService.class);
        textExtractionService = org.mockito.Mockito.mock(TextExtractionService.class);
        textChunkingService = org.mockito.Mockito.mock(TextChunkingService.class);
        documentChunkRepository = org.mockito.Mockito.mock(DocumentChunkRepository.class);
        documentService = new DocumentService(
                userRepository, documentRepository, fileStorageService, textExtractionService,
                new DocumentUploadProperties(10_485_760), textChunkingService, documentChunkRepository);

        user = new User();
        user.setId(7L);
        user.setEmail("owner@example.com");
        user.setName("Owner");
        user.setRole(Role.USER);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadsDocumentAndPersistsReadyText() throws Exception {
        when(fileStorageService.store(any())).thenReturn("trusted-key");
        when(fileStorageService.open("trusted-key"))
                .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any())).thenReturn("hello");

        DocumentResponse response = documentService.upload(
                "owner@example.com", file("notes.txt", "hello", "text/plain"));

        assertEquals(DocumentProcessingStatus.READY, response.processingStatus());
        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        KnowledgeDocument saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(DocumentProcessingStatus.READY, saved.getProcessingStatus());
        assertEquals("hello", saved.getExtractedText());
    }

    @Test
    void extractionFailurePersistsFailedStatusAndKeepsFile() throws Exception {
        when(fileStorageService.store(any())).thenReturn("trusted-key");
        when(fileStorageService.open("trusted-key"))
                .thenReturn(new ByteArrayInputStream("bad".getBytes(StandardCharsets.UTF_8)));
        when(textExtractionService.extract(any(), any()))
                .thenThrow(new TextExtractionException("No meaningful text found"));

        DocumentResponse response = documentService.upload(
                "owner@example.com", file("broken.txt", "bad", "text/plain"));

        assertEquals(DocumentProcessingStatus.FAILED, response.processingStatus());
        assertEquals("No meaningful text found", response.extractionError());
        org.mockito.Mockito.verify(fileStorageService, org.mockito.Mockito.never()).delete("trusted-key");
    }

    @Test
    void listsOnlyDocumentsForResolvedUser() {
        KnowledgeDocument document = readyDocument();
        when(documentRepository.findByUserIdOrderByCreatedAtDesc(7L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(document)));

        var page = documentService.list("owner@example.com", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        verify(documentRepository).findByUserIdOrderByCreatedAtDesc(7L, PageRequest.of(0, 10));
    }

    @Test
    void rejectsCrossUserDocumentAccess() {
        when(documentRepository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.get("owner@example.com", 99L));
    }

    @Test
    void returnsTextForOwnedReadyDocument() {
        KnowledgeDocument document = readyDocument();
        when(documentRepository.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(document));

        DocumentTextResponse response = documentService.getText("owner@example.com", 4L);

        assertEquals("extracted", response.text());
    }

    @Test
    void deletesPhysicalFileAndOwnedDatabaseRecord() {
        KnowledgeDocument document = readyDocument();
        document.setStorageKey("trusted-key");
        when(documentRepository.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(document));

        documentService.delete("owner@example.com", 4L);

        verify(fileStorageService).delete("trusted-key");
        verify(documentRepository).delete(document);
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

    private KnowledgeDocument readyDocument() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(4L);
        document.setUser(user);
        document.setOriginalFileName("notes.txt");
        document.setDocumentType(com.enterpriseai.backend.entity.DocumentType.TXT);
        document.setProcessingStatus(DocumentProcessingStatus.READY);
        document.setExtractedText("extracted");
        return document;
    }

    private MockMultipartFile file(String name, String content, String contentType) {
        return new MockMultipartFile("file", name, contentType,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
