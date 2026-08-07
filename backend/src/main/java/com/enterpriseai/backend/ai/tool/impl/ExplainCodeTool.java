package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class ExplainCodeTool implements Tool {
    @Override public String getId() { return "explain_code"; }
    @Override public String getName() { return "Explain Code"; }
    @Override public String getDescription() { return "Explains source code"; }
    @Override public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("codeSnippet", "string", "Code to explain", true));
    }
    @Override public String getCategory() { return "Development"; }
    @Override public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        return new ToolResult(true, "Simulated execution of Explain Code. Parameters: " + parameters);
    }
}
