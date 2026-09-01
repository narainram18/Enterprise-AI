package com.enterpriseai.backend.workspace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import com.enterpriseai.backend.workspace.entity.Workspace;
import com.enterpriseai.backend.workspace.service.WorkspaceService;
import com.enterpriseai.backend.common.ApiResponse;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getCreatedAt(), workspace.getUpdatedAt(), workspace.isOnlineMode());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listWorkspaces(
            Authentication authentication) {
        List<WorkspaceResponse> responses = workspaceService.listWorkspaces(authentication.getName())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspaces retrieved successfully", responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            Authentication authentication,
            @RequestBody WorkspaceRequest request) {
        Workspace workspace = workspaceService.createWorkspace(request.name(), authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace created successfully", toResponse(workspace)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            Authentication authentication,
            @PathVariable Long id) {
        Workspace workspace = workspaceService.getWorkspace(id, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace retrieved successfully", toResponse(workspace)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> renameWorkspace(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody WorkspaceRequest request) {
        Workspace workspace = workspaceService.renameWorkspace(id, request.name(), authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace renamed successfully", toResponse(workspace)));
    }

    @PatchMapping("/{id}/mode")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspaceMode(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody WorkspaceModeRequest request) {
        Workspace workspace = workspaceService.updateWorkspaceMode(id, request.onlineMode(), authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace mode updated successfully", toResponse(workspace)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
            Authentication authentication,
            @PathVariable Long id) {
        workspaceService.deleteWorkspace(id, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace deleted successfully", null));
    }
}
