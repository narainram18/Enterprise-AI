package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class TaskPlannerTool implements Tool {
    @Override public String getId() { return "task_planner"; }
    @Override public String getName() { return "Task Planner"; }
    @Override public String getDescription() { return "Creates task plans"; }
    @Override public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("goal", "string", "Project goal", true));
    }
    @Override public String getCategory() { return "Project Management"; }
    @Override public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        return new ToolResult(true, "Simulated execution of Task Planner. Parameters: " + parameters);
    }
}
