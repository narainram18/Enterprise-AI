package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

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
public class CodeAnalysisTool implements Tool {

    private final AiProvider aiProvider;

    public CodeAnalysisTool(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "code_analysis";
    }

    @Override
    public String getName() {
        return "Code Analysis";
    }

    @Override
    public String getDescription() {
        return "Analyzes source code for bugs, code smells, security issues, and provides improvements.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("codeSnippet", "string", "Code to analyze", true));
    }

    @Override
    public String getCategory() {
        return "Development";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String codeSnippet = (String) parameters.get("codeSnippet");
        if (codeSnippet == null || codeSnippet.isBlank()) {
            return new ToolResult(false, "Error: codeSnippet parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert code reviewer and security auditor. " +
                                "Analyze the following code snippet. Identify any bugs, code smells, performance issues, " +
                                "and security vulnerabilities. Return a structured markdown response with suggestions. " +
                                "Do NOT execute the code, just perform static analysis."
                            ),
                            new AiMessage(AiMessageRole.USER, "Code to analyze:\n\n" + codeSnippet)
                    ),
                    0.2, // low temp for analytical task
                    0.9,
                    null
            );

            String analysisResult = aiProvider.generate(request);
            
            if (analysisResult == null || analysisResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, analysisResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to analyze code: " + ex.getMessage());
        }
    }
}

