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
public class TaskPlannerTool implements Tool {

    private final AiProvider aiProvider;

    public TaskPlannerTool(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "task_planner";
    }

    @Override
    public String getName() {
        return "Task Planner";
    }

    @Override
    public String getDescription() {
        return "Creates detailed, structured project plans based on a goal.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("goal", "string", "Project goal", true));
    }

    @Override
    public String getCategory() {
        return "Project Management";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String goal = (String) parameters.get("goal");
        if (goal == null || goal.isBlank()) {
            return new ToolResult(false, "Error: goal parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert Project Manager. " +
                                "Given a project goal, create a clear, ordered, and detailed task plan. " +
                                "Include dependencies where appropriate, expected results for each step, " +
                                "and potential risks or blockers. Format the output as a Markdown list " +
                                "containing structured JSON for each step if possible, or just a highly " +
                                "structured markdown representation that a machine/agent could easily read."
                            ),
                            new AiMessage(AiMessageRole.USER, "Project Goal:\n" + goal)
                    ),
                    0.3, 
                    0.9,
                    null
            );

            String planResult = aiProvider.generate(request);
            
            if (planResult == null || planResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, planResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to create task plan: " + ex.getMessage());
        }
    }
}

