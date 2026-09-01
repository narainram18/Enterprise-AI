package com.enterpriseai.backend.workspace.context;

import com.enterpriseai.backend.workspace.entity.WorkspaceRole;

public class WorkspaceContext {
    private final Long workspaceId;
    private final WorkspaceRole role;
    private final boolean onlineMode;

    public WorkspaceContext(Long workspaceId, WorkspaceRole role, boolean onlineMode) {
        this.workspaceId = workspaceId;
        this.role = role;
        this.onlineMode = onlineMode;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }
}
