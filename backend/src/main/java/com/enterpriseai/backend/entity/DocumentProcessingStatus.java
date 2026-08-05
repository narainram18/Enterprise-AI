package com.enterpriseai.backend.entity;

public enum DocumentProcessingStatus {
    UPLOADED,
    EXTRACTING_TEXT,
    CHUNKING,
    CREATING_EMBEDDINGS,
    STORING_VECTORS,
    PROCESSING, // Legacy support if needed
    READY,
    FAILED
}
