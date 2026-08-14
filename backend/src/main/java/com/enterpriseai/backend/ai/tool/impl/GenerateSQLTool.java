package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class GenerateSQLTool implements Tool {

    private final AiProvider aiProvider;
    private static final Pattern DESTRUCTIVE_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE)\\b"
    );

    public GenerateSQLTool(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "generate_sql";
    }

    @Override
    public String getName() {
        return "Generate SQL";
    }

    @Override
    public String getDescription() {
        return "Generates safe, READ-ONLY SQL queries based on a natural language requirement and a provided schema.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            new ToolParameter("schema", "string", "Database schema context", true),
            new ToolParameter("requirement", "string", "What data to query", true)
        );
    }

    @Override
    public String getCategory() {
        return "Database";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String schema = (String) parameters.get("schema");
        String requirement = (String) parameters.get("requirement");
        
        if (schema == null || requirement == null || requirement.isBlank()) {
            return new ToolResult(false, "Error: Both schema and requirement parameters are required.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert SQL developer. Given a database schema, " +
                                "write a SQL query to satisfy the requirement. " +
                                "CRITICAL SECURITY RULES: " +
                                "1. You MUST ONLY generate a SELECT query. " +
                                "2. DO NOT use markdown formatting (no ```sql). " +
                                "3. Return ONLY the raw SQL text. " +
                                "4. Use only the tables and columns provided in the schema."
                            ),
                            new AiMessage(AiMessageRole.USER, "Schema:\n" + schema + "\n\nRequirement:\n" + requirement)
                    ),
                    0.1, // very low temp for deterministic SQL
                    0.9,
                    null
            );

            String generatedSql = aiProvider.generate(request);
            
            if (generatedSql == null || generatedSql.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }
            
            // Clean up any markdown blocks if the LLM hallucinated them despite instructions
            generatedSql = generatedSql.replaceAll("(?s)```sql\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            // Strict security validation: reject any destructive keywords
            if (DESTRUCTIVE_SQL_PATTERN.matcher(generatedSql).find()) {
                return new ToolResult(false, "SECURITY VIOLATION: Generated SQL contains unauthorized operations. Only SELECT is allowed.");
            }
            
            // Final check just to be absolutely sure it starts with SELECT or WITH
            String upperSql = generatedSql.toUpperCase();
            if (!upperSql.startsWith("SELECT ") && !upperSql.startsWith("WITH ")) {
                return new ToolResult(false, "SECURITY VIOLATION: Query must begin with SELECT or WITH.");
            }

            return new ToolResult(true, generatedSql);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to generate SQL: " + ex.getMessage());
        }
    }
}

