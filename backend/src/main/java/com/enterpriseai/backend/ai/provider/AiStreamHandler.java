package com.enterpriseai.backend.ai.provider;

public interface AiStreamHandler {

    void onToken(String token);

    boolean isCancelled();
}
