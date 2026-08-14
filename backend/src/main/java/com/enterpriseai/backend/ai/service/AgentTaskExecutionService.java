package com.enterpriseai.backend.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.ai.exception.AiStreamCancelledException;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolExecutor;
import com.enterpriseai.backend.ai.tool.ToolResult;
import com.enterpriseai.backend.entity.AgentTask;
import com.enterpriseai.backend.entity.AgentTaskStatus;
import com.enterpriseai.backend.repository.AgentTaskRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;

@Service
public class AgentTaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskExecutionService.class);
    
    private final AgentTaskRepository taskRepository;
    private final AgentRegistry agentRegistry;
    private final AiProvider aiProvider;
    private final ToolExecutor toolExecutor;

    public AgentTaskExecutionService(
            AgentTaskRepository taskRepository,
            AgentRegistry agentRegistry,
            AiProvider aiProvider,
            ToolExecutor toolExecutor) {
        this.taskRepository = taskRepository;
        this.agentRegistry = agentRegistry;
        this.aiProvider = aiProvider;
        this.toolExecutor = toolExecutor;
    }

    @Async("aiStreamingExecutor")
    public void executeTask(Long taskId, WorkspaceContext context) {
        // Set the context for this background thread
        WorkspaceContextHolder.setContext(context);
        
        AgentTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("Task {} not found", taskId);
            WorkspaceContextHolder.clearContext();
            return;
        }

        try {
            task.setStatus(AgentTaskStatus.RUNNING);
            taskRepository.save(task);

            Agent agent = agentRegistry.getAgent(task.getAgentId());
            if (agent == null) {
                throw new IllegalArgumentException("Unknown agent: " + task.getAgentId());
            }

            List<AiMessage> messages = new ArrayList<>();
            messages.add(new AiMessage(AiMessageRole.SYSTEM, agent.systemPrompt()));
            messages.add(new AiMessage(AiMessageRole.USER, task.getPrompt()));

            AiChatRequest request = new AiChatRequest(messages, agent.temperature(), agent.topP(), agent.model());
            
            boolean loop = true;
            int iterations = 0;
            int maxIterations = 10;
            
            StringBuilder finalOutput = new StringBuilder();

            while (loop && iterations < maxIterations) {
                loop = false;
                iterations++;
                
                StringBuilder buffer = new StringBuilder();
                
                ToolStreamInterceptor interceptor = new ToolStreamInterceptor(new AiStreamHandler() {
                    @Override
                    public void onToken(String token) {
                        buffer.append(token);
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                });

                boolean completed = aiProvider.stream(request, interceptor);
                interceptor.flushRemaining();

                if (!completed) {
                    throw new RuntimeException("AI provider stream failed or cancelled");
                }

                if (interceptor.hasToolCall()) {
                    String toolName = interceptor.getToolName();
                    log.info("Task {} invoking tool: {}", taskId, toolName);
                    
                    ToolContext toolContext = new ToolContext(
                            task.getWorkspace().getId(), 
                            task.getCreatedBy(), 
                            null // no conversation id for background task
                    );
                    
                    ToolResult result = toolExecutor.execute(toolName, interceptor.getToolParameters(), toolContext);
                    
                    // Add assistant's tool call message
                    List<AiMessage> newMessages = new ArrayList<>(request.messages());
                    newMessages.add(new AiMessage(AiMessageRole.ASSISTANT, interceptor.getRawToolCall()));
                    
                    // Add the result back as a user message
                    String toolResultMessage = "<tool_result>\n" +
                            "  <success>" + result.success() + "</success>\n" +
                            "  <result>" + result.content() + "</result>\n" +
                            "</tool_result>\n";
                    newMessages.add(new AiMessage(AiMessageRole.USER, toolResultMessage));
                    
                    request = new AiChatRequest(newMessages, request.temperature(), request.topP(), request.model());
                    loop = true; // continue the loop
                } else {
                    finalOutput.append(buffer.toString());
                }
            }
            
            if (iterations >= maxIterations) {
                throw new RuntimeException("Agent reached maximum iterations (" + maxIterations + ")");
            }

            task.setResult(finalOutput.toString());
            task.setStatus(AgentTaskStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("Task {} failed: {}", taskId, e.getMessage(), e);
            task.setStatus(AgentTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        } finally {
            taskRepository.save(task);
            WorkspaceContextHolder.clearContext();
        }
    }
}
