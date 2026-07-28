package com.enterpriseai.backend.ai.exception;

public class AiRetrievalException extends RuntimeException {

    public AiRetrievalException(String message) {
        super(message);
    }

    public AiRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
