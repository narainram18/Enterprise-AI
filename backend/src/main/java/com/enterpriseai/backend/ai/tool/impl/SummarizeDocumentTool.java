package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class SummarizeDocumentTool implements Tool {

    private final DocumentService documentService;
    private final AiProvider aiProvider;

    public SummarizeDocumentTool(DocumentService documentService, @Lazy AiProvider aiProvider) {
        this.documentService = documentService;
        this.aiProvider = aiProvider;
    }

    @Override
    public String getId() {
        return "summarize_document";
    }

    @Override
    public String getName() {
        return "Summarize Document";
    }

    @Override
    public String getDescription() {
        return "Summarizes a complete document based on its ID.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            new ToolParameter("documentId", "string", "ID of the document to summarize", true)
        );
    }

    @Override
    public String getCategory() {
        return "Document Analysis";
    }

    @Override
    public boolean requiresInternet() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        Object docIdObj = parameters.get("documentId");
        if (docIdObj == null || docIdObj.toString().isBlank()) {
            return new ToolResult(false, "Error: documentId parameter is required and cannot be empty.");
        }

        Long documentId;
        try {
            documentId = Long.valueOf(docIdObj.toString());
        } catch (NumberFormatException e) {
            return new ToolResult(false, "Error: documentId must be a valid number.");
        }

        try {
            DocumentTextResponse docResponse = documentService.getText(context.email(), documentId);
            String fullText = docResponse.text();

            if (fullText == null || fullText.isBlank()) {
                return new ToolResult(false, "Document has no text content to summarize.");
            }

            // Truncate to avoid context window explosion (e.g. 10,000 chars approx)
            if (fullText.length() > 10000) {
                fullText = fullText.substring(0, 10000) + "\n...[Content truncated for summary]...";
            }

            AiChatRequest request = new AiChatRequest(
                    List.of(
                            new AiMessage(AiMessageRole.SYSTEM, 
                                "You are an expert Research Analyst. " +
                                "Summarize the provided document text. " +
                                "Highlight the main theme, key points, and actionable conclusions. " +
                                "Format the summary cleanly with markdown bullet points."
                            ),
                            new AiMessage(AiMessageRole.USER, "Document Text:\n\n" + fullText)
                    ),
                    0.2, // Low temp for factual summary
                    0.9,
                    null
            );

            String summaryResult = aiProvider.generate(request);
            
            if (summaryResult == null || summaryResult.isBlank()) {
                return new ToolResult(false, "AI provider returned an empty response.");
            }

            return new ToolResult(true, summaryResult);
        } catch (Exception ex) {
            return new ToolResult(false, "Failed to summarize document: " + ex.getMessage());
        }
    }
}
