package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentDetailsResponse;
import com.enterpriseai.backend.workspace.entity.Workspace;
import com.enterpriseai.backend.workspace.service.WorkspaceService;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class WorkspaceInfoTool implements Tool {

    private final WorkspaceService workspaceService;
    private final DocumentService documentService;

    public WorkspaceInfoTool(WorkspaceService workspaceService, DocumentService documentService) {
        this.workspaceService = workspaceService;
        this.documentService = documentService;
    }

    @Override
    public String getId() {
        return "workspace_info";
    }

    @Override
    public String getName() {
        return "Workspace Information";
    }

    @Override
    public String getDescription() {
        return "Retrieves information about the current workspace, including its name and the number of documents uploaded.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of();
    }

    @Override
    public String getCategory() {
        return "Workspace";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        try {
            Workspace workspace = workspaceService.getWorkspace(context.workspaceId(), context.email());
            Page<DocumentDetailsResponse> docs = documentService.list(context.email(), PageRequest.of(0, 1), null, null);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Workspace Name: ").append(workspace.getName()).append("\n");
            sb.append("Total Documents: ").append(docs.getTotalElements()).append("\n");
            
            return new ToolResult(true, sb.toString());
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to get workspace info: " + ex.getMessage());
        }
    }
}
