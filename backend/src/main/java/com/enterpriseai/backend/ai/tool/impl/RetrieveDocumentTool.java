package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class RetrieveDocumentTool implements Tool {

    private final DocumentService documentService;

    public RetrieveDocumentTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public String getId() {
        return "retrieve_document";
    }

    @Override
    public String getName() {
        return "Retrieve Document";
    }

    @Override
    public String getDescription() {
        return "Retrieves the full extracted text content of a specific document by its ID.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("documentId", "number", "The ID of the document to retrieve.", true)
        );
    }

    @Override
    public String getCategory() {
        return "Document Retrieval";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        Object docIdObj = parameters.get("documentId");
        if (docIdObj == null) {
            return new ToolResult(false, "documentId parameter is required.");
        }

        Long documentId;
        try {
            documentId = Long.valueOf(docIdObj.toString());
        } catch (NumberFormatException e) {
            return new ToolResult(false, "documentId must be a valid number.");
        }

        try {
            DocumentTextResponse response = documentService.getText(context.email(), documentId);
            return new ToolResult(true, "Document Text:\n" + response.text());
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to retrieve document: " + ex.getMessage());
        }
    }
}
