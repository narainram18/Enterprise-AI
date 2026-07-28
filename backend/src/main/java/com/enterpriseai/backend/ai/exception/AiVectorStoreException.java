package com.enterpriseai.backend.ai.exception;

public class AiVectorStoreException extends RuntimeException {

    public AiVectorStoreException(String message) {
        super(message);
    }

    public AiVectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
