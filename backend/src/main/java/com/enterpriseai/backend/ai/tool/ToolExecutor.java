package com.enterpriseai.backend.ai.tool;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);
    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolResult execute(String toolId, Map<String, Object> parameters, ToolContext context) {
        if (toolId == null) {
            log.warn("Attempted to execute tool with null ID");
            return new ToolResult(false, "Invalid XML: tool_name is missing or malformed.");
        }
        Tool tool = toolRegistry.getTool(toolId);
        if (tool == null) {
            log.warn("Attempted to execute unknown tool: {}", toolId);
            return new ToolResult(false, "Unknown tool: " + toolId);
        }

        com.enterpriseai.backend.workspace.context.WorkspaceContext workspaceContext = com.enterpriseai.backend.workspace.context.WorkspaceContextHolder.getContext();
        boolean isOnline = workspaceContext != null && workspaceContext.isOnlineMode();
        
        if (tool.requiresInternet() && !isOnline) {
            log.warn("Attempted to execute internet-dependent tool {} while workspace is offline", toolId);
            return new ToolResult(false, "Tool execution blocked: Workspace is in Offline Mode and this tool requires internet access.");
        }

        try {
            log.info("Executing tool: {} with parameters: {} and context: {}", toolId, parameters, context);
            
            return tool.execute(parameters, context);
        } catch (Exception ex) {
            log.error("Tool execution failed: {}", toolId, ex);
            return new ToolResult(false, "Execution failed: " + ex.getMessage());
        }
    }
}
