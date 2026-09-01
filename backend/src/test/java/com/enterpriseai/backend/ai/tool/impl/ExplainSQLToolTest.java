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

class ExplainSQLToolTest {

    private AiProvider aiProvider;
    private ExplainSQLTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        tool = new ExplainSQLTool(aiProvider);
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_validInput_returnsExplanation() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("This query selects all users.");

        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM users;");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertEquals("This query selects all users.", result.content());
    }

    @Test
    void execute_emptyQuery_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required and cannot be empty"));
    }

    @Test
    void execute_missingQuery_returnsError() {
        Map<String, Object> params = new HashMap<>();

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required and cannot be empty"));
    }

    @Test
    void execute_providerReturnsEmpty_returnsError() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("   ");

        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM users;");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("empty response"));
    }

    @Test
    void execute_providerThrowsException_returnsError() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenThrow(new RuntimeException("Provider failure"));

        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM users;");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Provider failure"));
    }
}
