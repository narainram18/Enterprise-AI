package com.enterpriseai.backend.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.context.DefaultChatContextExtension;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.tool.impl.BrowserTool;
import com.enterpriseai.backend.ai.tool.impl.CodeAnalysisTool;
import com.enterpriseai.backend.ai.tool.impl.WebSearchTool;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.entity.WorkspaceRole;

class OfflineModeEnforcementTest {

    private ToolExecutor toolExecutor;
    private DefaultChatContextExtension chatContextExtension;
    private WebSearchTool webSearchTool;
    private BrowserTool browserTool;
    private CodeAnalysisTool localTool;

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        webSearchTool = mock(WebSearchTool.class);
        when(webSearchTool.getId()).thenReturn("web_search");
        when(webSearchTool.requiresInternet()).thenReturn(true);
        when(webSearchTool.execute(any(), any())).thenReturn(new ToolResult(true, "Search Result"));
        
        browserTool = mock(BrowserTool.class);
        when(browserTool.getId()).thenReturn("browser_tool");
        when(browserTool.requiresInternet()).thenReturn(true);
        when(browserTool.execute(any(), any())).thenReturn(new ToolResult(true, "Browser Result"));
        
        localTool = mock(CodeAnalysisTool.class);
        when(localTool.getId()).thenReturn("code_analysis");
        when(localTool.requiresInternet()).thenReturn(false);
        when(localTool.execute(any(), any())).thenReturn(new ToolResult(true, "Code Result"));

        registry = new ToolRegistry(List.of(webSearchTool, browserTool, localTool));

        toolExecutor = new ToolExecutor(registry);
        chatContextExtension = new DefaultChatContextExtension(mock(RagPromptBuilder.class), registry);
    }

    private void setWorkspaceMode(boolean isOnline) {
        WorkspaceContextHolder.setContext(new WorkspaceContext(1L, WorkspaceRole.OWNER, isOnline));
    }

    @Test
    void testA_offlineWorkspaceAgentAttemptsWebSearchTool() {
        setWorkspaceMode(false);
        
        Agent agent = mock(Agent.class);
        when(agent.supportedTools()).thenReturn(List.of("web_search", "code_analysis"));
        
        RagPromptBuilder promptBuilder = mock(RagPromptBuilder.class);
        AiChatRequest request = new AiChatRequest(List.of(new AiMessage(AiMessageRole.USER, "search web")), 0.7, 1.0, "model");
        when(promptBuilder.build(any(), any(), any())).thenReturn(request);
        
        DefaultChatContextExtension extension = new DefaultChatContextExtension(promptBuilder, registry);
        AiChatRequest modifiedRequest = extension.extend(agent, request, "");
        
        String systemPrompt = modifiedRequest.messages().get(0).content();
        
        // Agent should not even see the web_search tool in offline mode
        assertThat(systemPrompt).doesNotContain("**web_search**");
        // Agent SHOULD see local tools
        assertThat(systemPrompt).contains("**code_analysis**");
        // Agent should see the offline warning
        assertThat(systemPrompt).contains("OFFLINE MODE");
    }

    @Test
    void testB_offlineWorkspaceAgentAttemptsBrowserTool() {
        setWorkspaceMode(false);
        
        Agent agent = mock(Agent.class);
        when(agent.supportedTools()).thenReturn(List.of("browser_tool"));
        
        RagPromptBuilder promptBuilder = mock(RagPromptBuilder.class);
        AiChatRequest request = new AiChatRequest(List.of(new AiMessage(AiMessageRole.USER, "browse site")), 0.7, 1.0, "model");
        when(promptBuilder.build(any(), any(), any())).thenReturn(request);
        
        DefaultChatContextExtension extension = new DefaultChatContextExtension(promptBuilder, registry);
        AiChatRequest modifiedRequest = extension.extend(agent, request, "");
        
        String systemPrompt = modifiedRequest.messages().get(0).content();
        
        // Agent should not see the browser_tool in offline mode
        assertThat(systemPrompt).doesNotContain("**browser_tool**");
    }

    @Test
    void testC_offlineWorkspaceDirectToolExecutorInvocationOfInternetToolRejected() {
        setWorkspaceMode(false);
        ToolContext context = new ToolContext(1L, "user@test.com", null);
        
        ToolResult result = toolExecutor.execute("web_search", Map.of(), context);
        
        assertThat(result.success()).isFalse();
        assertThat(result.content()).contains("Tool execution blocked");
        assertThat(result.content()).contains("Offline Mode");
    }

    @Test
    void testD_onlineWorkspaceWebSearchToolAllowed() {
        setWorkspaceMode(true);
        ToolContext context = new ToolContext(1L, "user@test.com", null);
        
        ToolResult result = toolExecutor.execute("web_search", Map.of(), context);
        
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("Search Result");
    }

    @Test
    void testE_onlineWorkspaceBrowserToolAllowed() {
        setWorkspaceMode(true);
        ToolContext context = new ToolContext(1L, "user@test.com", null);
        
        ToolResult result = toolExecutor.execute("browser_tool", Map.of(), context);
        
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("Browser Result");
    }

    @Test
    void testF_offlineWorkspaceLocalToolsContinueWorking() {
        setWorkspaceMode(false);
        ToolContext context = new ToolContext(1L, "user@test.com", null);
        
        ToolResult result = toolExecutor.execute("code_analysis", Map.of(), context);
        
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("Code Result");
    }
}
