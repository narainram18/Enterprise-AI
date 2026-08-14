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

class GenerateSQLToolTest {

    private AiProvider aiProvider;
    private GenerateSQLTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        tool = new GenerateSQLTool(aiProvider);
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_validSelect_returnsSql() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("SELECT * FROM users WHERE active = true;");

        Map<String, Object> params = new HashMap<>();
        params.put("schema", "Table users(id, active)");
        params.put("requirement", "Get all active users");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertEquals("SELECT * FROM users WHERE active = true;", result.content());
    }

    @Test
    void execute_markdownSelect_stripsMarkdownAndReturnsSql() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("```sql\nSELECT * FROM users;\n```");

        Map<String, Object> params = new HashMap<>();
        params.put("schema", "Table users");
        params.put("requirement", "Get all users");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertEquals("SELECT * FROM users;", result.content());
    }

    @Test
    void execute_destructiveSql_isRejected() {
        String[] maliciousQueries = {
            "DROP TABLE users;",
            "DELETE FROM users WHERE id = 1;",
            "UPDATE users SET role = 'admin';",
            "TRUNCATE TABLE logs;",
            "ALTER TABLE users DROP COLUMN password;",
            "CREATE TABLE temp (id int);",
            "select * from users; drop table users;"
        };

        for (String query : maliciousQueries) {
            when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(query);

            Map<String, Object> params = new HashMap<>();
            params.put("schema", "Table users");
            params.put("requirement", "Do bad things");

            ToolResult result = tool.execute(params, context);

            assertFalse(result.success(), "Query should have been rejected: " + query);
            assertTrue(result.content().contains("SECURITY VIOLATION"), "Expected security violation message");
        }
    }

    @Test
    void execute_doesNotStartWithSelectOrWith_isRejected() {
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("EXPLAIN SELECT * FROM users;");

        Map<String, Object> params = new HashMap<>();
        params.put("schema", "Table users");
        params.put("requirement", "Explain plan");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("must begin with SELECT or WITH"));
    }

    @Test
    void execute_missingParameters_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("schema", "Table users"); // missing requirement

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Both schema and requirement parameters are required"));
    }
}
