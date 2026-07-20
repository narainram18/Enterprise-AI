package com.enterpriseai.backend.ai;

import java.util.List;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OllamaAiProviderIntegrationTest {

    @Autowired
    private AiProvider aiProvider;

    @Test
    void shouldGenerateResponseFromOllama() {

        String response = aiProvider.generate(
                new AiChatRequest(List.of(
                        new AiMessage(
                                AiMessageRole.USER,
                                "Reply with exactly these two words: OLLAMA WORKS"
                        )
                ))
        );

        System.out.println("AI RESPONSE: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
