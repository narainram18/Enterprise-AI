package com.enterpriseai.backend.ai.provider.ollama;

import java.util.List;

import com.enterpriseai.backend.ai.config.OllamaEmbeddingProperties;
import com.enterpriseai.backend.ai.exception.AiEmbeddingException;
import com.enterpriseai.backend.ai.provider.ollama.dto.OllamaEmbedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaEmbeddingProviderTest {

    private MockRestServiceServer mockServer;
    private OllamaEmbeddingProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        OllamaEmbeddingProperties properties = new OllamaEmbeddingProperties("http://localhost:11434", "test-model");
        provider = new OllamaEmbeddingProvider(builder, properties);
        objectMapper = new ObjectMapper();
    }

    @Test
    void embed_rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> provider.embed(null));
    }

    @Test
    void embed_rejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> provider.embed("   "));
    }

    @Test
    void embedBatch_rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(null));
    }

    @Test
    void embedBatch_rejectsEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(List.of()));
    }

    @Test
    void embedBatch_rejectsListWithNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(List.of("valid", "")));
        String nullString = null;
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(java.util.Arrays.asList("valid", nullString)));
    }

    @Test
    void embed_success() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse("test-model", List.of(List.of(0.1, 0.2, 0.3)));
        String responseBody = objectMapper.writeValueAsString(mockResponse);

        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<Double> result = provider.embed("hello");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(0.1, result.get(0));
        mockServer.verify();
    }

    @Test
    void embedBatch_success() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse("test-model", List.of(List.of(0.1), List.of(0.2)));
        String responseBody = objectMapper.writeValueAsString(mockResponse);

        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<List<Double>> result = provider.embedBatch(List.of("hello", "world"));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0.1, result.get(0).get(0));
        assertEquals(0.2, result.get(1).get(0));
        mockServer.verify();
    }

    @Test
    void embed_handles404() {
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        AiEmbeddingException ex = assertThrows(AiEmbeddingException.class, () -> provider.embed("hello"));
        assertEquals("Embedding model is unavailable", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embed_handles400() {
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        AiEmbeddingException ex = assertThrows(AiEmbeddingException.class, () -> provider.embed("hello"));
        assertEquals("AI provider rejected the request", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embed_handles500() {
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        AiEmbeddingException ex = assertThrows(AiEmbeddingException.class, () -> provider.embed("hello"));
        assertEquals("AI provider request failed", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embed_handlesEmptyResponse() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse("test-model", List.of());
        String responseBody = objectMapper.writeValueAsString(mockResponse);

        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiEmbeddingException ex = assertThrows(AiEmbeddingException.class, () -> provider.embed("hello"));
        assertEquals("AI provider returned an invalid response", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embed_handlesEmptyVector() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse("test-model", List.of(List.of()));
        String responseBody = objectMapper.writeValueAsString(mockResponse);

        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiEmbeddingException ex = assertThrows(AiEmbeddingException.class, () -> provider.embed("hello"));
        assertEquals("AI provider returned an invalid vector", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embedBatch_rejectsResponseCountMismatch() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse("test-model", List.of(List.of(0.1)));
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiEmbeddingException ex = assertThrows(
                AiEmbeddingException.class,
                () -> provider.embedBatch(List.of("hello", "world")));
        assertEquals("AI provider returned an embedding count mismatch", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embedBatch_rejectsInconsistentDimensions() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse(
                "test-model", List.of(List.of(0.1), List.of(0.2, 0.3)));
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiEmbeddingException ex = assertThrows(
                AiEmbeddingException.class,
                () -> provider.embedBatch(List.of("hello", "world")));
        assertEquals("AI provider returned inconsistent vector dimensions", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void embedBatch_rejectsNonFiniteValues() throws Exception {
        OllamaEmbedResponse mockResponse = new OllamaEmbedResponse(
                "test-model", List.of(List.of(Double.NaN)));
        mockServer.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiEmbeddingException ex = assertThrows(
                AiEmbeddingException.class,
                () -> provider.embedBatch(List.of("hello")));
        assertEquals("AI provider returned a vector with invalid values", ex.getMessage());
        mockServer.verify();
    }
}
