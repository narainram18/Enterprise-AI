package com.enterpriseai.backend.dto;

import com.enterpriseai.backend.entity.DocumentProcessingStatus;

public record DocumentTextResponse(
        Long id,
        String originalFileName,
        DocumentProcessingStatus processingStatus,
        String text) {
}
