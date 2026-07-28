package com.enterpriseai.backend.ai.context;

import com.enterpriseai.backend.ai.model.AiChatRequest;

public interface ChatContextExtension {

    AiChatRequest extend(AiChatRequest historyRequest, String retrievalContext);
}
