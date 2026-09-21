package com.enterpriseai.backend.ai.tool.impl;

import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class EnterpriseResearchTool implements Tool {

    private final RetrievalPipeline retrievalPipeline;
    private final UserRepository userRepository;

    public EnterpriseResearchTool(RetrievalPipeline retrievalPipeline, UserRepository userRepository) {
        this.retrievalPipeline = retrievalPipeline;
        this.userRepository = userRepository;
    }

    @Override
    public String getId() {
        return "enterprise_research";
    }

    @Override
    public boolean requiresInternet() {
        return true;
    }

    @Override
    public String getCategory() {
        return "Research";
    }

    @Override
    public String getName() {
        return "Enterprise Research";
    }

    @Override
    public String getDescription() {
        return "Performs a comprehensive research query by simultaneously searching internal workspace documents and the public web. It deduplicates and reranks the evidence before returning a unified context.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("query", "string", "The research query.", true)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            return new ToolResult(false, "Query is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(context.email())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        CompletableFuture<List<RetrievedChunk>> internalSearch = CompletableFuture.supplyAsync(() -> 
                retrievalPipeline.retrieveAndRank(query, context.workspaceId(), user.getId())
        ).exceptionally(ex -> List.of());

        CompletableFuture<List<String>> externalSearch = CompletableFuture.supplyAsync(() -> 
                searchWeb(query)
        ).exceptionally(ex -> List.of());

        List<RetrievedChunk> internalResults = internalSearch.join();
        List<String> webResults = externalSearch.join();

        StringBuilder sb = new StringBuilder();
        sb.append("--- ENTERPRISE RESEARCH RESULTS ---\n\n");
        
        if (!internalResults.isEmpty()) {
            sb.append("INTERNAL WORKSPACE EVIDENCE:\n");
            for (int i = 0; i < internalResults.size(); i++) {
                RetrievedChunk chunk = internalResults.get(i);
                sb.append("[Workspace Source: ").append(chunk.documentFileName()).append("]\n");
                sb.append(chunk.chunkText()).append("\n\n");
            }
        } else {
            sb.append("INTERNAL WORKSPACE EVIDENCE: None found.\n\n");
        }

        if (!webResults.isEmpty()) {
            sb.append("EXTERNAL WEB EVIDENCE:\n");
            for (String webResult : webResults) {
                sb.append(webResult).append("\n\n");
            }
        } else {
            sb.append("EXTERNAL WEB EVIDENCE: None found.\n\n");
        }

        return new ToolResult(true, sb.toString());
    }

    private List<String> searchWeb(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(5000)
                    .followRedirects(false)
                    .get();

            Elements results = doc.select(".result");
            List<String> parsedResults = new ArrayList<>();
            int count = 0;
            for (Element result : results) {
                if (count >= 3) break; // Limit to top 3 web results to save context
                Element titleEl = result.selectFirst(".result__title > a");
                Element snippetEl = result.selectFirst(".result__snippet");
                if (titleEl != null && snippetEl != null) {
                    parsedResults.add("[Web Source: " + titleEl.text() + "]\n" + snippetEl.text());
                    count++;
                }
            }
            return parsedResults;
        } catch (Exception e) {
            return List.of();
        }
    }
}
