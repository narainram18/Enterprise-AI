package com.enterpriseai.backend.ai.context;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.model.AiChatRequest;

@Component
public class DefaultChatContextExtension implements ChatContextExtension {

    private final RagPromptBuilder promptBuilder;
    private final com.enterpriseai.backend.ai.tool.ToolRegistry toolRegistry;

    public DefaultChatContextExtension(RagPromptBuilder promptBuilder, com.enterpriseai.backend.ai.tool.ToolRegistry toolRegistry) {
        this.promptBuilder = promptBuilder;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public AiChatRequest extend(com.enterpriseai.backend.ai.agent.Agent agent, AiChatRequest historyRequest, String retrievalContext) {
        AiChatRequest baseRequest = promptBuilder.build(agent, historyRequest, retrievalContext);
        
        if (agent.supportedTools() == null || agent.supportedTools().isEmpty()) {
            return baseRequest;
        }

        StringBuilder toolPrompt = new StringBuilder();
        toolPrompt.append("\n\n# TOOLS\n");
        toolPrompt.append("You have access to the following tools. You can use them to fetch real-time information or perform actions.\n");
        toolPrompt.append("To call a tool, output a single XML block EXACTLY like this:\n");
        toolPrompt.append("<invoke_tool>\n");
        toolPrompt.append("  <tool_name>tool_id_here</tool_name>\n");
        toolPrompt.append("  <parameters>\n");
        toolPrompt.append("    <your_parameter_name>parameter_value</your_parameter_name>\n");
        toolPrompt.append("  </parameters>\n");
        toolPrompt.append("</invoke_tool>\n\n");
        toolPrompt.append("Available Tools:\n");

        for (String toolId : agent.supportedTools()) {
            com.enterpriseai.backend.ai.tool.Tool tool = toolRegistry.getTool(toolId);
            if (tool != null) {
                toolPrompt.append("- **").append(tool.getId()).append("**: ").append(tool.getDescription()).append("\n");
                for (com.enterpriseai.backend.ai.tool.ToolParameter param : tool.getParameters()) {
                    toolPrompt.append("  - Parameter `").append(param.name()).append("` (").append(param.type()).append("): ").append(param.description());
                    if (param.required()) toolPrompt.append(" [REQUIRED]");
                    toolPrompt.append("\n");
                }
                toolPrompt.append("\n");
            }
        }
        
        toolPrompt.append("If you output a <invoke_tool>, STOP generation immediately. I will reply with the tool result.\n");

        // Prepend tool prompt to the existing system prompt
        java.util.List<com.enterpriseai.backend.ai.model.AiMessage> newMessages = new java.util.ArrayList<>();
        if (!baseRequest.messages().isEmpty() && baseRequest.messages().get(0).role() == com.enterpriseai.backend.ai.model.AiMessageRole.SYSTEM) {
            String newSystemContent = baseRequest.messages().get(0).content() + toolPrompt.toString();
            newMessages.add(new com.enterpriseai.backend.ai.model.AiMessage(com.enterpriseai.backend.ai.model.AiMessageRole.SYSTEM, newSystemContent));
            newMessages.addAll(baseRequest.messages().subList(1, baseRequest.messages().size()));
        } else {
            newMessages.add(new com.enterpriseai.backend.ai.model.AiMessage(com.enterpriseai.backend.ai.model.AiMessageRole.SYSTEM, toolPrompt.toString()));
            newMessages.addAll(baseRequest.messages());
        }

        return new AiChatRequest(newMessages, baseRequest.temperature(), baseRequest.topP(), baseRequest.model());
    }
}
