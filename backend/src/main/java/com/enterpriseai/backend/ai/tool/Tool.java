package com.enterpriseai.backend.ai.tool;

import java.util.List;
import java.util.Map;

public interface Tool {

    String getId();

    String getName();

    String getDescription();

    List<ToolParameter> getParameters();

    String getCategory();

    ToolResult execute(Map<String, Object> parameters, ToolContext context);
}
