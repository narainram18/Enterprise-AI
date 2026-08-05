package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;

import com.enterpriseai.backend.entity.DocumentProcessingStatus;
import com.enterpriseai.backend.entity.DocumentType;

public record DocumentDetailsResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        DocumentType documentType,
        DocumentProcessingStatus processingStatus,
        String extractionError,
        String uploaderName,
        String workspaceName,
        int chunkCount,
        LocalDateTime lastIndexedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
