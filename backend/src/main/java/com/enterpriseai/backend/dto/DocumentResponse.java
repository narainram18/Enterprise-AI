package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;

import com.enterpriseai.backend.entity.DocumentProcessingStatus;
import com.enterpriseai.backend.entity.DocumentType;

public record DocumentResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        Integer version,
        DocumentType documentType,
        DocumentProcessingStatus processingStatus,
        String extractionError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
