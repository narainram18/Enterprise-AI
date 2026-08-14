package com.enterpriseai.backend.ai.tool.impl;

import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    private WebSearchTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new WebSearchTool();
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_validInput_returnsResults() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "Spring Boot framework");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertNotNull(result.content());
        assertTrue(result.content().contains("Spring Boot"));
    }

    @Test
    void execute_emptyQuery_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "   ");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required"));
    }

    @Test
    void execute_missingQuery_returnsError() {
        Map<String, Object> params = new HashMap<>();

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required"));
    }
}
