package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
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
public class ExplainSQLTool implements Tool {

    private final AiProvider aiProvider;

    public ExplainSQLTool(@Lazy AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "explain_sql";
    }

    @Override
    public String getName() {
        return "Explain SQL";
    }

    @Override
    public String getDescription() {
        return "Explains complex SQL queries, breaking down joins, subqueries, and potential performance implications.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("query", "string", "SQL query to explain", true));
    }

    @Override
    public String getCategory() {
        return "Database";
    }

    @Override
    public boolean requiresInternet() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String query = (String) parameters.get("query");
        
        if (query == null || query.isBlank()) {
            return new ToolResult(false, "Error: query parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert Database Administrator. " +
                                "Explain the provided SQL query. Break down each clause, explain the purpose of joins, " +
                                "and highlight any potential performance bottlenecks or risks (e.g. missing indexes, full table scans). " +
                                "Provide your response in structured markdown with headings. " +
                                "Do NOT execute the query."
                            ),
                            new AiMessage(AiMessageRole.USER, "SQL Query to explain:\n\n```sql\n" + query + "\n```")
                    ),
                    0.2, // Low temp for accurate database explanation
                    0.9,
                    null
            );

            String explanationResult = aiProvider.generate(request);
            
            if (explanationResult == null || explanationResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, explanationResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to explain SQL query: " + ex.getMessage());
        }
    }
}
