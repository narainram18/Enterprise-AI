package com.enterpriseai.backend.ai.tool.impl;

import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WebSearchTool implements Tool {

    @Override
    public String getId() {
        return "web_search";
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
        return "Web Search";
    }

    @Override
    public String getDescription() {
        return "Search the web for real-time information, news, or factual answers. Use this when you don't know the answer or when the user explicitly asks to search the web.";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("query", "string", "The search query to look up on the web.", true),
                new ToolParameter("maxResults", "integer", "Maximum number of results to return (default 5, max 10).", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String query = (String) parameters.get("query");
        if (query == null || query.trim().isEmpty()) {
            return new ToolResult(false, "Search query is required.");
        }

        int maxResults = 5;
        if (parameters.containsKey("maxResults")) {
            try {
                Object maxResultsObj = parameters.get("maxResults");
                if (maxResultsObj instanceof Number) {
                    maxResults = ((Number) maxResultsObj).intValue();
                } else if (maxResultsObj instanceof String) {
                    maxResults = Integer.parseInt((String) maxResultsObj);
                }
            } catch (Exception e) {
                // Ignore and use default
            }
        }
        maxResults = Math.min(Math.max(1, maxResults), 10); // clamp between 1 and 10

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(5000)
                    .get();

            Elements results = doc.select(".result");

            if (results.isEmpty()) {
                return new ToolResult(true, "No results found for query: " + query);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Web Search Results for \"").append(query).append("\":\n\n");

            int count = 0;
            for (Element result : results) {
                if (count >= maxResults) break;

                Element titleEl = result.selectFirst(".result__title > a");
                Element snippetEl = result.selectFirst(".result__snippet");
                Element urlEl = result.selectFirst(".result__url");

                if (titleEl != null && snippetEl != null) {
                    String title = titleEl.text();
                    String link = titleEl.attr("href");
                    String snippet = snippetEl.text();

                    if (link.startsWith("//duckduckgo.com/l/?uddg=")) {
                        // Extract real URL if wrapped by DuckDuckGo redirect
                        try {
                            String encodedUrl = link.replace("//duckduckgo.com/l/?uddg=", "").split("&")[0];
                            link = java.net.URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            // ignore decoding errors
                        }
                    } else if (urlEl != null && !link.startsWith("http")) {
                        link = "https://" + urlEl.text().trim();
                    }

                    sb.append("### ").append(title).append("\n");
                    sb.append("**URL**: ").append(link).append("\n");
                    sb.append("**Snippet**: ").append(snippet).append("\n\n");
                    count++;
                }
            }

            if (count == 0) {
                return new ToolResult(true, "No valid results could be extracted.");
            }

            return new ToolResult(true, sb.toString());
        } catch (Exception e) {
            // DuckDuckGo failed, fallback to Wikipedia OpenSearch API
            try {
                String wikiUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + maxResults + "&format=json";

                Document wikiDoc = Jsoup.connect(wikiUrl)
                        .userAgent("EnterpriseAI/1.0 (test@enterpriseai.com)")
                        .ignoreContentType(true)
                        .timeout(5000)
                        .get();

                String json = wikiDoc.body().text();
                // Basic JSON array parsing since we don't have Jackson here directly
                // Format: ["query", ["title1", "title2"], ["snippet1", ""], ["url1", "url2"]]

                // For simplicity, we just use string manipulation or a simple regex since Jackson is not injected
                if (json.contains("[") && json.length() > 20) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Web Search Results (Wikipedia Fallback) for \"").append(query).append("\":\n\n");

                    // A simple and robust way to parse this without a library is to just use standard java classes,
                    // but since Jackson is available in the classpath, let's just use it!
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.List<?> list = mapper.readValue(json, java.util.List.class);

                    if (list.size() >= 4) {
                        java.util.List<String> titles = (java.util.List<String>) list.get(1);
                        java.util.List<String> snippets = (java.util.List<String>) list.get(2);
                        java.util.List<String> urls = (java.util.List<String>) list.get(3);

                        for (int i = 0; i < titles.size(); i++) {
                            sb.append("### ").append(titles.get(i)).append("\n");
                            sb.append("**URL**: ").append(urls.get(i)).append("\n");
                            if (i < snippets.size() && !snippets.get(i).isEmpty()) {
                                sb.append("**Snippet**: ").append(snippets.get(i)).append("\n\n");
                            } else {
                                sb.append("**Snippet**: ").append(titles.get(i)).append(" article.\n\n");
                            }
                        }

                        if (titles.isEmpty()) {
                            return new ToolResult(true, "No results found on Wikipedia for query: " + query);
                        }
                        return new ToolResult(true, sb.toString());
                    }
                }
                return new ToolResult(false, "Search failed and fallback returned invalid data.");

            } catch (Exception ex) {
                return new ToolResult(false, "Web search failed. DuckDuckGo error: " + e.getMessage() + ". Wikipedia fallback error: " + ex.getMessage());
            }
        }
    }
}
