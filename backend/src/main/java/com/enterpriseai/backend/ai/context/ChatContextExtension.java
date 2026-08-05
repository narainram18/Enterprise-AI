package com.enterpriseai.backend.ai.context;

import com.enterpriseai.backend.ai.model.AiChatRequest;

public interface ChatContextExtension {

    AiChatRequest extend(com.enterpriseai.backend.ai.agent.Agent agent, AiChatRequest historyRequest, String retrievalContext);
}
