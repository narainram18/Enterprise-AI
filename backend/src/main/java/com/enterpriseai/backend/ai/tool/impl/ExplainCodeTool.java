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
public class ExplainCodeTool implements Tool {

    private final AiProvider aiProvider;

    public ExplainCodeTool(@Lazy AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "explain_code";
    }

    @Override
    public String getName() {
        return "Explain Code";
    }

    @Override
    public String getDescription() {
        return "Explains source code clearly, detailing its purpose, logic, and potential edge cases.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("codeSnippet", "string", "Code to explain", true));
    }

    @Override
    public String getCategory() {
        return "Development";
    }

    @Override
    public boolean requiresInternet() {
        return false;
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
                                "You are an expert software engineer and technical educator. " +
                                "Explain the following code snippet. Break down the logic step-by-step, " +
                                "explain its overall purpose, and highlight any important edge cases. " +
                                "Use markdown headings and bullet points for clarity. " +
                                "Do NOT execute the code."
                            ),
                            new AiMessage(AiMessageRole.USER, "Code to explain:\n\n" + codeSnippet)
                    ),
                    0.3, // Low temp for factual explanation
                    0.9,
                    null
            );

            String explanationResult = aiProvider.generate(request);
            
            if (explanationResult == null || explanationResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, explanationResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to explain code: " + ex.getMessage());
        }
    }
}
