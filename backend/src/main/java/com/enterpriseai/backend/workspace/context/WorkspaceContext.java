package com.enterpriseai.backend.workspace.context;

import com.enterpriseai.backend.workspace.entity.WorkspaceRole;

public class WorkspaceContext {
    private final Long workspaceId;
    private final WorkspaceRole role;

    public WorkspaceContext(Long workspaceId, WorkspaceRole role) {
        this.workspaceId = workspaceId;
        this.role = role;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public WorkspaceRole getRole() {
        return role;
    }
}
