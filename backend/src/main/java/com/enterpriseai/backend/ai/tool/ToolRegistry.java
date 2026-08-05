package com.enterpriseai.backend.ai.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(Collection<Tool> availableTools) {
        for (Tool tool : availableTools) {
            tools.put(tool.getId(), tool);
        }
    }

    public void register(Tool tool) {
        tools.put(tool.getId(), tool);
    }

    public Tool getTool(String id) {
        return tools.get(id);
    }

    public Collection<Tool> getTools() {
        return Collections.unmodifiableCollection(tools.values());
    }
}
