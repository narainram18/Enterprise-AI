package com.enterpriseai.backend.ai.tool.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;

class CodeAnalysisToolTest {

    private AiProvider aiProvider;
    private CodeAnalysisTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        tool = new CodeAnalysisTool(aiProvider);
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_validInput_returnsAnalysis() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("Here is your analysis: No bugs found.");

        Map<String, Object> params = new HashMap<>();
        params.put("codeSnippet", "public class Main { }");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertEquals("Here is your analysis: No bugs found.", result.content());
    }

    @Test
    void execute_emptyCodeSnippet_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("codeSnippet", "");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required and cannot be empty"));
    }

    @Test
    void execute_missingCodeSnippet_returnsError() {
        Map<String, Object> params = new HashMap<>();

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required and cannot be empty"));
    }

    @Test
    void execute_providerReturnsEmpty_returnsError() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("   ");

        Map<String, Object> params = new HashMap<>();
        params.put("codeSnippet", "public class Main { }");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("empty response"));
    }

    @Test
    void execute_providerThrowsException_returnsError() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenThrow(new RuntimeException("Ollama is down"));

        Map<String, Object> params = new HashMap<>();
        params.put("codeSnippet", "public class Main { }");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Ollama is down"));
    }
}
