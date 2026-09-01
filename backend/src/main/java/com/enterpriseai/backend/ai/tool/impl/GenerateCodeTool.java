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
public class GenerateCodeTool implements Tool {

    private final AiProvider aiProvider;

    public GenerateCodeTool(@Lazy AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "generate_code";
    }

    @Override
    public String getName() {
        return "Generate Code";
    }

    @Override
    public String getDescription() {
        return "Generates source code based on a prompt or requirements.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            new ToolParameter("prompt", "string", "Code requirements or instructions", true),
            new ToolParameter("language", "string", "Target programming language", false)
        );
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
        String prompt = (String) parameters.get("prompt");
        String language = (String) parameters.getOrDefault("language", "Unknown");

        if (prompt == null || prompt.isBlank()) {
            return new ToolResult(false, "Error: prompt parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert software engineer. " +
                                "Generate the requested source code. " +
                                "Follow best practices, add minimal but useful comments, and provide a brief explanation of any assumptions you made. " +
                                "Always format your code in markdown code blocks with the correct language tag. " +
                                "Do NOT execute the code, only generate text."
                            ),
                            new AiMessage(AiMessageRole.USER, "Target Language: " + language + "\n\nRequirements:\n" + prompt)
                    ),
                    0.2, // Low temp for structured, accurate code generation
                    0.9,
                    null
            );

            String generationResult = aiProvider.generate(request);
            
            if (generationResult == null || generationResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, generationResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to generate code: " + ex.getMessage());
        }
    }
}
