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
public class RiskAnalysisTool implements Tool {

    private final AiProvider aiProvider;

    public RiskAnalysisTool(@Lazy AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "risk_analysis";
    }

    @Override
    public String getName() {
        return "Risk Analysis";
    }

    @Override
    public String getDescription() {
        return "Analyzes project plans or tasks for potential risks and suggests mitigations.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("projectPlan", "string", "Project plan details", true));
    }

    @Override
    public String getCategory() {
        return "Project Management";
    }

    @Override
    public boolean requiresInternet() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String projectPlan = (String) parameters.get("projectPlan");
        
        if (projectPlan == null || projectPlan.isBlank()) {
            return new ToolResult(false, "Error: projectPlan parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are a specialized Risk Management Consultant. " +
                                "Analyze the provided project plan for potential risks. " +
                                "Consider technical, schedule, resource, and external risks. " +
                                "For each identified risk, define its Probability (Low/Medium/High), Impact (Low/Medium/High), " +
                                "and provide a concrete Mitigation Strategy. " +
                                "Format the output as a clear markdown table."
                            ),
                            new AiMessage(AiMessageRole.USER, "Project Plan to analyze:\n\n" + projectPlan)
                    ),
                    0.3, // Low temp for analytical task
                    0.9,
                    null
            );

            String analysisResult = aiProvider.generate(request);
            
            if (analysisResult == null || analysisResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, analysisResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to analyze risks: " + ex.getMessage());
        }
    }
}
