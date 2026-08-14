package com.enterpriseai.backend.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.dto.AgentTaskResponse;
import com.enterpriseai.backend.dto.CreateAgentTaskRequest;
import com.enterpriseai.backend.service.AgentTaskService;

@RestController
@RequestMapping("/api/agents/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgentTaskResponse>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAgentTaskRequest request) {
        
        AgentTaskResponse task = agentTaskService.createTask(userDetails.getUsername(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Task created successfully", task));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AgentTaskResponse>>> getTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AgentTaskResponse> tasks = agentTaskService.getTasks(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tasks fetched successfully", PageResponse.from(tasks)));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> getTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        
        AgentTaskResponse task = agentTaskService.getTask(taskId, userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Task fetched successfully", task));
    }
}
