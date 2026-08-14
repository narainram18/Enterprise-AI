package com.enterpriseai.backend.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.ai.tool.ToolExecutor;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;

public class ToolStreamInterceptor implements AiStreamHandler {

    private final AiStreamHandler delegate;
    private final StringBuilder buffer = new StringBuilder();
    private boolean inToolCall = false;
    private boolean toolCallComplete = false;
    private String toolName;
    private Map<String, Object> toolParameters = new HashMap<>();
    private String rawToolCall;

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("<tool_name>(.*?)</tool_name>", Pattern.DOTALL);
    private static final Pattern PARAM_PATTERN = Pattern.compile("<([^>]+)>([^<]*)</\\1>", Pattern.DOTALL);

    public ToolStreamInterceptor(AiStreamHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onToken(String token) {
        buffer.append(token);
        String currentStr = buffer.toString();

        if (currentStr.contains("<invoke_tool>")) {
            inToolCall = true;
        }

        if (inToolCall) {
            if (currentStr.contains("</invoke_tool>")) {
                toolCallComplete = true;
                rawToolCall = currentStr.substring(currentStr.indexOf("<invoke_tool>"), currentStr.indexOf("</invoke_tool>") + 14);
                parseToolCall(rawToolCall);
            }
        } else {
            // Not in a tool call, we can safely pass the token if we know it's not the start of one
            // Simple buffering strategy: if the buffer ends with part of "<invoke_tool>", wait.
            // For simplicity in this implementation, we just pass tokens if they don't contain '<' 
            // and we flush when it's clear it's not a tool call.
            
            // To be perfectly accurate without delaying tokens, it's tricky.
            // Let's do a simple approach: if buffer has no '<', flush immediately.
            if (!currentStr.contains("<")) {
                delegate.onToken(currentStr);
                buffer.setLength(0);
            } else if (currentStr.contains("<") && !currentStr.contains("<invoke_tool>") && currentStr.length() > 20) {
                // Not a tool call, just some other xml or long string
                delegate.onToken(currentStr);
                buffer.setLength(0);
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    public boolean hasToolCall() {
        return toolCallComplete;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getToolParameters() {
        return toolParameters;
    }

    public String getRawToolCall() {
        return rawToolCall;
    }
    
    public void flushRemaining() {
        if (!inToolCall && buffer.length() > 0) {
            delegate.onToken(buffer.toString());
            buffer.setLength(0);
        }
    }

    private void parseToolCall(String xml) {
        Matcher nameMatcher = TOOL_NAME_PATTERN.matcher(xml);
        if (nameMatcher.find()) {
            toolName = nameMatcher.group(1).trim();
        }

        int paramsStart = xml.indexOf("<parameters>");
        int paramsEnd = xml.indexOf("</parameters>");
        if (paramsStart != -1 && paramsEnd != -1) {
            String paramsStr = xml.substring(paramsStart + 12, paramsEnd);
            Matcher paramMatcher = PARAM_PATTERN.matcher(paramsStr);
            while (paramMatcher.find()) {
                String key = paramMatcher.group(1).trim();
                String value = paramMatcher.group(2).trim();
                toolParameters.put(key, value);
            }
        }
    }
}
