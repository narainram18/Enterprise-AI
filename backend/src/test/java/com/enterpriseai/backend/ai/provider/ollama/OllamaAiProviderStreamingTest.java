package com.enterpriseai.backend.ai.provider.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.ai.config.OllamaProperties;

class OllamaAiProviderStreamingTest {

    @Test
    void shouldParseNdjsonChunksInOrderAndUseStreamingRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAiProvider provider = new OllamaAiProvider(
                builder,
                new OllamaProperties("http://localhost:11434", "test-model"),
                new com.fasterxml.jackson.databind.ObjectMapper());

        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "model": "test-model",
                          "messages": [{"role": "user", "content": "Hello"}],
                          "stream": true
                        }
                        """))
                .andRespond(withSuccess("""
                        {"message":{"role":"assistant","content":"First "},"done":false}
                        {"message":{"role":"assistant","content":"second"},"done":false}
                        {"message":{"role":"assistant","content":""},"done":true}
                        """, MediaType.APPLICATION_NDJSON));

        List<String> tokens = new ArrayList<>();
        boolean completed = provider.stream(
                new AiChatRequest(List.of(new AiMessage(AiMessageRole.USER, "Hello")), null, null, null),
                new AiStreamHandler() {
                    @Override
                    public void onToken(String token) {
                        tokens.add(token);
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                });

        server.verify();
        assertTrue(completed);
        assertEquals(List.of("First ", "second"), tokens);
    }
}
