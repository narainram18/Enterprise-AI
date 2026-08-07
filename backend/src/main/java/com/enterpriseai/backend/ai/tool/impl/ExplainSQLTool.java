package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class ExplainSQLTool implements Tool {
    @Override public String getId() { return "explain_sql"; }
    @Override public String getName() { return "Explain SQL"; }
    @Override public String getDescription() { return "Explains SQL queries"; }
    @Override public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("query", "string", "SQL query", true));
    }
    @Override public String getCategory() { return "Database"; }
    @Override public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        return new ToolResult(true, "Simulated execution of Explain SQL. Parameters: " + parameters);
    }
}
