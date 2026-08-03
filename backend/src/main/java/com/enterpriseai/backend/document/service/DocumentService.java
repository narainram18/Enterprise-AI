package com.enterpriseai.backend.document.service;

import java.io.InputStream;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.enterpriseai.backend.document.config.DocumentUploadProperties;
import com.enterpriseai.backend.document.extractor.TextExtractionException;
import com.enterpriseai.backend.document.extractor.TextExtractionService;
import com.enterpriseai.backend.document.storage.FileStorageService;
import com.enterpriseai.backend.dto.DocumentResponse;
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.entity.DocumentProcessingStatus;
import com.enterpriseai.backend.entity.DocumentType;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.BadRequestException;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.repository.DocumentChunkRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.repository.WorkspaceRepository;
import com.enterpriseai.backend.workspace.entity.Workspace;
import com.enterpriseai.backend.document.chunking.TextChunkingService;
import com.enterpriseai.backend.entity.DocumentChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final int MAX_ERROR_LENGTH = 500;

    private final UserRepository userRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final DocumentUploadProperties uploadProperties;
    private final TextChunkingService textChunkingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final WorkspaceRepository workspaceRepository;

    public DocumentService(
            UserRepository userRepository,
            KnowledgeDocumentRepository documentRepository,
            FileStorageService fileStorageService,
            TextExtractionService textExtractionService,
            DocumentUploadProperties uploadProperties,
            TextChunkingService textChunkingService,
            DocumentChunkRepository documentChunkRepository,
            DocumentEmbeddingService documentEmbeddingService,
            WorkspaceRepository workspaceRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
        this.uploadProperties = uploadProperties;
        this.textChunkingService = textChunkingService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentEmbeddingService = documentEmbeddingService;
        this.workspaceRepository = workspaceRepository;
    }

    @CacheEvict(value = "retrievalResults", allEntries = true)
    public DocumentResponse upload(String email, MultipartFile file) {
        log.info("CACHE EVICT - Invalidation triggered by upload");
        DocumentType type = validate(file);
        User user = resolveUser(email);
        WorkspaceContext context = WorkspaceContextHolder.getContext();
        Workspace workspace = workspaceRepository.getReferenceById(context.getWorkspaceId());
        
        String storageKey = fileStorageService.store(file);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setCreatedBy(user);
        document.setWorkspace(workspace);
        document.setOriginalFileName(originalName(file));
        document.setStorageKey(storageKey);
        document.setContentType(contentType(file));
        document.setFileSize(file.getSize());
        document.setDocumentType(type);
        document.setProcessingStatus(DocumentProcessingStatus.UPLOADED);

        try {
            documentRepository.save(document);
            document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);
            documentRepository.save(document);

            try (InputStream input = fileStorageService.open(storageKey)) {
                log.info("Upload started for file: {}", document.getOriginalFileName());
                String extractedText = textExtractionService.extract(type, input);
                log.info("Text extracted: {} chars", extractedText.length());
                document.setExtractedText(extractedText);
                document.setExtractionError(null);
                
                List<DocumentChunk> chunks = textChunkingService.createChunks(document, extractedText);
                log.info("Chunks created: {}", chunks.size());
                List<DocumentChunk> persistedChunks = documentChunkRepository.saveAll(chunks);
                log.info("Chunks persisted");
                
                log.info("Embedding generation started");
                documentEmbeddingService.embedAndStore(document, persistedChunks);
                log.info("Vectors stored and READY");
                
                document.setProcessingStatus(DocumentProcessingStatus.READY);
            } catch (java.io.IOException | RuntimeException ex) {
                log.error("Document processing failed", ex);
                if (document.getId() != null) {
                    documentChunkRepository.deleteByDocumentId(document.getId());
                }
                document.setExtractedText(null);
                document.setExtractionError(safeError(ex));
                document.setProcessingStatus(DocumentProcessingStatus.FAILED);
            }

            return toResponse(documentRepository.save(document));
        } catch (RuntimeException ex) {
            fileStorageService.delete(storageKey);
            throw ex;
        }
    }

    public Page<DocumentResponse> list(String email, Pageable pageable) {
        WorkspaceContext context = WorkspaceContextHolder.getContext();
        return documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(context.getWorkspaceId(), pageable)
                .map(this::toResponse);
    }

    public DocumentResponse get(String email, Long id) {
        return toResponse(findOwned(email, id));
    }

    public DocumentTextResponse getText(String email, Long id) {
        KnowledgeDocument document = findOwned(email, id);
        if (document.getProcessingStatus() != DocumentProcessingStatus.READY) {
            throw new BadRequestException("Document text is not available");
        }
        return new DocumentTextResponse(
                document.getId(), document.getOriginalFileName(),
                document.getProcessingStatus(), document.getExtractedText());
    }

    @CacheEvict(value = "retrievalResults", allEntries = true)
    public void delete(String email, Long id) {
        log.info("CACHE EVICT - Invalidation triggered by delete");
        KnowledgeDocument document = findOwned(email, id);
        documentEmbeddingService.deleteVectors(document);
        fileStorageService.delete(document.getStorageKey());
        documentRepository.delete(document);
    }

    private DocumentType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (uploadProperties.maxSize() <= 0 || file.getSize() > uploadProperties.maxSize()) {
            throw new BadRequestException("File exceeds the maximum allowed size");
        }

        String name = originalName(file);
        int dot = name.lastIndexOf('.');
        if (dot < 1 || dot == name.length() - 1) {
            throw new BadRequestException("File extension is required");
        }
        DocumentType type = switch (name.substring(dot + 1).toLowerCase(Locale.ROOT)) {
            case "pdf" -> DocumentType.PDF;
            case "docx" -> DocumentType.DOCX;
            case "txt" -> DocumentType.TXT;
            default -> throw new BadRequestException("Unsupported document format");
        };
        validateContentType(type, file.getContentType());
        return type;
    }

    private void validateContentType(DocumentType type, String contentType) {
        if (contentType == null || contentType.isBlank()
                || DEFAULT_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return;
        }
        boolean valid = switch (type) {
            case PDF -> "application/pdf".equalsIgnoreCase(contentType);
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    .equalsIgnoreCase(contentType);
            case TXT -> "text/plain".equalsIgnoreCase(contentType)
                    || contentType.toLowerCase(Locale.ROOT).startsWith("text/");
        };
        if (!valid) {
            throw new BadRequestException("File content type does not match its extension");
        }
    }

    private KnowledgeDocument findOwned(String email, Long id) {
        WorkspaceContext context = WorkspaceContextHolder.getContext();
        return documentRepository.findByIdAndWorkspaceId(id, context.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace"));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private DocumentResponse toResponse(KnowledgeDocument document) {
        return new DocumentResponse(
                document.getId(), document.getOriginalFileName(), document.getContentType(),
                document.getFileSize(), document.getDocumentType(), document.getProcessingStatus(),
                document.getExtractionError(), document.getCreatedAt(), document.getUpdatedAt());
    }

    private String originalName(MultipartFile file) {
        return file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "unnamed-file" : file.getOriginalFilename();
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? DEFAULT_CONTENT_TYPE : file.getContentType();
    }

    private String safeError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Document text extraction failed";
        }
        return message.length() > MAX_ERROR_LENGTH
                ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
