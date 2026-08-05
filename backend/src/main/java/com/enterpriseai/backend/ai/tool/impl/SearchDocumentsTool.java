package com.enterpriseai.backend.ai.tool.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.retrieval.service.SemanticSearchService;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;

@Component
public class SearchDocumentsTool implements Tool {

    private final SemanticSearchService semanticSearchService;

    public SearchDocumentsTool(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }

    @Override
    public String getId() {
        return "search_documents";
    }

    @Override
    public String getName() {
        return "Search Documents";
    }

    @Override
    public String getDescription() {
        return "Searches uploaded workspace documents for relevant information using semantic similarity.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("query", "string", "The search query to find information about.", true)
        );
    }

    @Override
    public String getCategory() {
        return "Document Retrieval";
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            return new ToolResult(false, "Query parameter is required.");
        }

        try {
            List<RetrievedChunk> chunks = semanticSearchService.search(query, context.workspaceId());
            if (chunks.isEmpty()) {
                return new ToolResult(true, "No matching documents found for query: " + query);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(chunks.size()).append(" relevant chunks:\n\n");
            for (int i = 0; i < chunks.size(); i++) {
                RetrievedChunk chunk = chunks.get(i);
                sb.append("--- Match ").append(i + 1).append(" ---\n");
                sb.append("Source: ").append(chunk.documentFileName()).append("\n");
                sb.append("Relevance: ").append(Math.round(chunk.similarityScore() * 100)).append("%\n");
                if (chunk.pageNumber() != null) {
                    sb.append("Page: ").append(chunk.pageNumber()).append("\n");
                }
                sb.append("Content:\n").append(chunk.chunkText()).append("\n\n");
            }

            return new ToolResult(true, sb.toString());
        } catch (Exception ex) {
            return new ToolResult(false, "Search failed: " + ex.getMessage());
        }
    }
}
