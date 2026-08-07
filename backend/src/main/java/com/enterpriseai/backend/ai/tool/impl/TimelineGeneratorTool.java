package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class TimelineGeneratorTool implements Tool {
    @Override public String getId() { return "timeline_generator"; }
    @Override public String getName() { return "Timeline Generator"; }
    @Override public String getDescription() { return "Generates project timelines"; }
    @Override public List<ToolParameter> getParameters() {
        return List.of(new ToolParameter("tasks", "string", "List of tasks", true));
    }
    @Override public String getCategory() { return "Project Management"; }
    @Override public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        return new ToolResult(true, "Simulated execution of Timeline Generator. Parameters: " + parameters);
    }
}
