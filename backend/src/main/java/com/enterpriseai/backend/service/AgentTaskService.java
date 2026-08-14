package com.enterpriseai.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterpriseai.backend.ai.service.AgentTaskExecutionService;
import com.enterpriseai.backend.dto.AgentTaskResponse;
import com.enterpriseai.backend.dto.CreateAgentTaskRequest;
import com.enterpriseai.backend.entity.AgentTask;
import com.enterpriseai.backend.entity.AgentTaskStatus;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.mapper.AgentTaskMapper;
import com.enterpriseai.backend.repository.AgentTaskRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.entity.Workspace;
import com.enterpriseai.backend.workspace.service.WorkspaceService;

@Service
public class AgentTaskService {

    private final AgentTaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final AgentTaskMapper taskMapper;
    private final AgentTaskExecutionService executionService;

    public AgentTaskService(
            AgentTaskRepository taskRepository,
            WorkspaceService workspaceService,
            AgentTaskMapper taskMapper,
            AgentTaskExecutionService executionService) {
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.taskMapper = taskMapper;
        this.executionService = executionService;
    }

    @Transactional
    public AgentTaskResponse createTask(String currentUserEmail, CreateAgentTaskRequest request) {
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        Workspace workspace = workspaceService.getWorkspace(context.getWorkspaceId(), currentUserEmail);

        AgentTask task = new AgentTask();
        task.setWorkspace(workspace);
        task.setAgentId(request.agentId());
        task.setCreatedBy(currentUserEmail);
        task.setPrompt(request.prompt());
        task.setStatus(AgentTaskStatus.PENDING);

        task = taskRepository.save(task);

        // Trigger background execution
        executionService.executeTask(task.getId(), context);

        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<AgentTaskResponse> getTasks(String currentUserEmail, Pageable pageable) {
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        Workspace workspace = workspaceService.getWorkspace(context.getWorkspaceId(), currentUserEmail);

        return taskRepository.findByWorkspaceOrderByCreatedAtDesc(workspace, pageable)
                .map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AgentTaskResponse getTask(Long taskId, String currentUserEmail) {
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        
        AgentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentTask not found: " + taskId));
                
        if (!task.getWorkspace().getId().equals(context.getWorkspaceId())) {
            throw new ResourceNotFoundException("AgentTask not found in this workspace");
        }

        return taskMapper.toResponse(task);
    }
}
