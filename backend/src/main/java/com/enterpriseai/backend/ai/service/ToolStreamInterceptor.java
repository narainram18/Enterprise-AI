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
            // Keep connection alive while buffering tool invocation
            delegate.onToken("");
            if (currentStr.contains("</invoke_tool>")) {
                toolCallComplete = true;
                rawToolCall = currentStr.substring(currentStr.indexOf("<invoke_tool>"), currentStr.indexOf("</invoke_tool>") + 14);
                parseToolCall(rawToolCall);
            }
        } else {
            if (!currentStr.contains("<")) {
                delegate.onToken(currentStr);
                buffer.setLength(0);
            } else if (currentStr.length() > 15 && !currentStr.contains("<invoke_tool>")) {
                // Not a tool call, just some other xml or long string
                delegate.onToken(currentStr);
                buffer.setLength(0);
            } else {
                // Still buffering potentially a <invoke_tool> tag
                delegate.onToken("");
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
