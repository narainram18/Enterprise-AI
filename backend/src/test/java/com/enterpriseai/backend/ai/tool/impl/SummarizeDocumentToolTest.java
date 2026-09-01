package com.enterpriseai.backend.ai.tool.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolResult;
import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentTextResponse;
import com.enterpriseai.backend.entity.DocumentProcessingStatus;

class SummarizeDocumentToolTest {

    private DocumentService documentService;
    private AiProvider aiProvider;
    private SummarizeDocumentTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        aiProvider = mock(AiProvider.class);
        tool = new SummarizeDocumentTool(documentService, aiProvider);
        context = new ToolContext(1L, "user@example.com", null);
    }

    @Test
    void execute_validInput_returnsSummary() throws Exception {
        when(documentService.getText(eq("user@example.com"), eq(123L)))
                .thenReturn(new DocumentTextResponse(123L, "test.pdf", DocumentProcessingStatus.READY, "This is a very long document text..."));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("This is the document summary.");

        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "123");

        ToolResult result = tool.execute(params, context);

        assertTrue(result.success());
        assertEquals("This is the document summary.", result.content());
    }

    @Test
    void execute_emptyDocumentId_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("required and cannot be empty"));
    }

    @Test
    void execute_invalidDocumentIdFormat_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "abc");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("valid number"));
    }

    @Test
    void execute_documentHasNoText_returnsError() throws Exception {
        when(documentService.getText(eq("user@example.com"), eq(123L)))
                .thenReturn(new DocumentTextResponse(123L, "test.pdf", DocumentProcessingStatus.READY, ""));

        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "123");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("has no text content"));
    }

    @Test
    void execute_providerReturnsEmpty_returnsError() throws Exception {
        when(documentService.getText(eq("user@example.com"), eq(123L)))
                .thenReturn(new DocumentTextResponse(123L, "test.pdf", DocumentProcessingStatus.READY, "Text"));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn("");

        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "123");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("empty response"));
    }

    @Test
    void execute_documentServiceThrowsException_returnsError() throws Exception {
        when(documentService.getText(eq("user@example.com"), eq(123L)))
                .thenThrow(new RuntimeException("Doc not found"));

        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "123");

        ToolResult result = tool.execute(params, context);

        assertFalse(result.success());
        assertTrue(result.content().contains("Doc not found"));
    }
}
