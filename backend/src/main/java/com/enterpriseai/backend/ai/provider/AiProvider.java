package com.enterpriseai.backend.ai.provider;

import com.enterpriseai.backend.ai.model.AiChatRequest;

public interface AiProvider {

    String generate(AiChatRequest request);

    boolean stream(AiChatRequest request, AiStreamHandler handler);
}
