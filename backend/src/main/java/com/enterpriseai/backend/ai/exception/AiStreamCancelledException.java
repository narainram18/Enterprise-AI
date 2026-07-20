package com.enterpriseai.backend.ai.exception;

public class AiStreamCancelledException extends RuntimeException {

    public AiStreamCancelledException() {
        super("AI stream cancelled");
    }
}
