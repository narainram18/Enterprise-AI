package com.enterpriseai.backend;

import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;
import com.enterpriseai.backend.ai.tool.impl.BrowserTool;

import java.util.Map;

public class TestBrowser {
    public static void main(String[] args) {
        System.out.println("Starting BrowserTool test...");
        BrowserTool tool = new BrowserTool();
        ToolResult result = tool.execute(Map.of("url", "https://example.com"), new ToolContext(1L, "test@example.com", 1L));
        System.out.println("Result Status: " + result.success());
        System.out.println("Result Data: " + result.content());
        tool.cleanup();
    }
}
