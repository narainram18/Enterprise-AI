package com.enterpriseai.backend.workspace.context;

public class WorkspaceContextHolder {

    private static final ThreadLocal<WorkspaceContext> contextHolder = new ThreadLocal<>();

    public static void setContext(WorkspaceContext context) {
        contextHolder.set(context);
    }

    public static WorkspaceContext getContext() {
        return contextHolder.get();
    }

    public static WorkspaceContext getRequiredContext() {
        WorkspaceContext context = contextHolder.get();
        if (context == null) {
            throw new com.enterpriseai.backend.exception.BadRequestException("Workspace context is missing. Please select a workspace.");
        }
        return context;
    }

    public static void clearContext() {
        contextHolder.remove();
    }
}
