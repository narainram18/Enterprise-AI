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
public class TimelineGeneratorTool implements Tool {

    private final AiProvider aiProvider;

    public TimelineGeneratorTool(@Lazy AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "timeline_generator";
    }

    @Override
    public String getName() {
        return "Timeline Generator";
    }

    @Override
    public String getDescription() {
        return "Generates structured project timelines and estimates based on a list of tasks.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("tasks", "string", "List of tasks or project plan details", true));
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
        String tasks = (String) parameters.get("tasks");
        
        if (tasks == null || tasks.isBlank()) {
            return new ToolResult(false, "Error: tasks parameter is required and cannot be empty.");
        }

        try {
            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert Agile Project Manager. " +
                                "Based on the provided list of tasks or project plan, generate a structured timeline. " +
                                "Organize the tasks chronologically, grouping them into Phases or Sprints. " +
                                "For each task, provide an estimated duration, identify dependencies, and flag milestones. " +
                                "Format the output as a clean markdown table."
                            ),
                            new AiMessage(AiMessageRole.USER, "Tasks to schedule:\n\n" + tasks)
                    ),
                    0.4, // Medium-low temp for structured creativity
                    0.9,
                    null
            );

            String timelineResult = aiProvider.generate(request);
            
            if (timelineResult == null || timelineResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, timelineResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to generate timeline: " + ex.getMessage());
        }
    }
}
