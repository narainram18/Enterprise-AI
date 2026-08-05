package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentDetailsResponse;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class ListDocumentsTool implements Tool {

    private final DocumentService documentService;

    public ListDocumentsTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public String getId() {
        return "list_documents";
    }

    @Override
    public String getName() {
        return "List Documents";
    }

    @Override
    public String getDescription() {
        return "Lists all documents uploaded to the current workspace, returning their IDs, names, and processing status.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of();
    }

    @Override
    public String getCategory() {
        return "Document Retrieval";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        try {
            Page<DocumentDetailsResponse> docs = documentService.list(context.email(), PageRequest.of(0, 100), null, null);
            if (docs.isEmpty()) {
                return new ToolResult(true, "No documents found in the workspace.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(docs.getTotalElements()).append(" documents:\n\n");
            for (DocumentDetailsResponse doc : docs.getContent()) {
                sb.append("- ID: ").append(doc.id()).append("\n");
                sb.append("  Name: ").append(doc.originalFileName()).append("\n");
                sb.append("  Type: ").append(doc.documentType()).append("\n");
                sb.append("  Status: ").append(doc.processingStatus()).append("\n");
                sb.append("  Uploaded: ").append(doc.createdAt()).append("\n\n");
            }
            
            return new ToolResult(true, sb.toString());
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to list documents: " + ex.getMessage());
        }
    }
}
