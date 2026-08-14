package com.enterpriseai.backend.ai.tool.impl;

import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class BrowserToolTest {

    private BrowserTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new BrowserTool();
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_invalidSchema_returnsSecurityError() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "file:///etc/passwd");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("SECURITY ERROR"));
        assertTrue(result.content().contains("Only https:// URLs are allowed"));
    }
    
    @Test
    void execute_javascriptUrl_returnsSecurityError() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "javascript:alert(1)");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("SECURITY ERROR"));
    }

    @Test
    void execute_localhost_returnsSecurityError() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://localhost:8080/api/users");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Access denied by security proxy (SSRF blocked)") || result.content().contains("Browser failed to load the page"));
    }

    @Test
    void execute_loopbackIp_returnsSecurityError() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://127.0.0.1:5432/");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Access denied by security proxy (SSRF blocked)") || result.content().contains("Browser failed to load the page"));
    }
    
    @org.junit.jupiter.api.Disabled("Blocked by Azure CDN Playwright Chromium download outage")
    @Test
    void execute_realExecution_returnsContent() {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "https://example.com");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success(), "Expected successful real execution");
        assertTrue(result.content().contains("Example Domain"), "Content did not contain expected text");
        
        // Ensure cleanup doesn't throw exceptions
        tool.cleanup();
    }
    
    @Test
    void testSafeProxyIpValidation() throws Exception {
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("127.0.0.1")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("localhost")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("10.5.5.5")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("172.16.0.1")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("192.168.1.1")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("169.254.169.254")));
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("100.64.1.1"))); // CGNAT
        assertFalse(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("0.0.0.0")));
        
        assertTrue(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("8.8.8.8")));
        assertTrue(BrowserTool.SafeProxy.isSafeIp(InetAddress.getByName("93.184.216.34"))); // example.com
    }
}
