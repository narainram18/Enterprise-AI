package com.enterpriseai.backend.ai.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.exception.AiGenerationException;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.service.ConversationService;

@ExtendWith(MockitoExtension.class)
class AiStreamingChatServiceTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private AiProvider aiProvider;

    private AiStreamingChatService streamingChatService;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        streamingChatService = new AiStreamingChatService(
                conversationService,
                aiChatService,
                conversationMapper,
                aiProvider,
                new AiChatProperties(2, 120000),
                directExecutor);
    }

    @Test
    void shouldStreamTokensInOrderAndPersistOneCompleteAssistantMessage() {
        CreateMessageRequest request = request("Explain SSE");
        ChatMessage userMessage = message(1L, MessageRole.USER, request.getContent());
        ChatMessage assistantMessage = message(2L, MessageRole.ASSISTANT, "First second");
        ChatMessageResponse userResponse = response(userMessage);
        ChatMessageResponse assistantResponse = response(assistantMessage);
        AiChatRequest aiRequest = new AiChatRequest(List.of(
                new AiMessage(AiMessageRole.USER, request.getContent())));

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(userMessage);
        when(aiChatService.buildContextRequest(42L)).thenReturn(aiRequest);
        when(conversationMapper.toMessageResponse(userMessage)).thenReturn(userResponse);
        when(aiProvider.stream(eq(aiRequest), any(AiStreamHandler.class)))
                .thenAnswer(invocation -> {
                    AiStreamHandler handler = invocation.getArgument(1);
                    handler.onToken("First ");
                    handler.onToken("second");
                    return true;
                });
        when(conversationService.saveAssistantMessage(
                42L,
                "user@example.com",
                "First second"))
                .thenReturn(assistantMessage);
        when(conversationMapper.toMessageResponse(assistantMessage)).thenReturn(assistantResponse);

        streamingChatService.stream(42L, "user@example.com", request);

        InOrder order = inOrder(conversationService, aiChatService, aiProvider);
        order.verify(conversationService).saveUserMessage(42L, "user@example.com", request);
        order.verify(aiChatService).buildContextRequest(42L);
        order.verify(aiProvider).stream(eq(aiRequest), any(AiStreamHandler.class));
        order.verify(conversationService).saveAssistantMessage(
                42L,
                "user@example.com",
                "First second");
        verify(conversationService).saveAssistantMessage(
                42L,
                "user@example.com",
                "First second");
    }

    @Test
    void shouldKeepUserMessageAndNotPersistAssistantWhenStreamingFails() {
        CreateMessageRequest request = request("This will fail");
        ChatMessage userMessage = message(1L, MessageRole.USER, request.getContent());

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(userMessage);
        when(aiChatService.buildContextRequest(42L))
                .thenReturn(new AiChatRequest(List.of(
                        new AiMessage(AiMessageRole.USER, request.getContent()))));
        when(conversationMapper.toMessageResponse(userMessage)).thenReturn(response(userMessage));
        when(aiProvider.stream(any(AiChatRequest.class), any(AiStreamHandler.class)))
                .thenThrow(new AiGenerationException("AI provider is unavailable"));

        streamingChatService.stream(42L, "user@example.com", request);

        verify(conversationService).saveUserMessage(42L, "user@example.com", request);
        verify(conversationService, never()).saveAssistantMessage(any(), any(), any());
    }

    @Test
    void shouldNotStartStreamingWhenConversationOwnershipValidationFails() {
        CreateMessageRequest request = request("Unauthorized");
        when(conversationService.saveUserMessage(42L, "other@example.com", request))
                .thenThrow(new ResourceNotFoundException("Conversation not found"));

        assertThrows(
                ResourceNotFoundException.class,
                () -> streamingChatService.stream(42L, "other@example.com", request));

        verifyNoInteractions(aiChatService, aiProvider, conversationMapper);
    }

    @Test
    void shouldNotPersistPartialAssistantWhenProviderReportsCancellation() {
        CreateMessageRequest request = request("Cancel this response");
        ChatMessage userMessage = message(1L, MessageRole.USER, request.getContent());

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(userMessage);
        when(aiChatService.buildContextRequest(42L))
                .thenReturn(new AiChatRequest(List.of(
                        new AiMessage(AiMessageRole.USER, request.getContent()))));
        when(conversationMapper.toMessageResponse(userMessage)).thenReturn(response(userMessage));
        when(aiProvider.stream(any(AiChatRequest.class), any(AiStreamHandler.class)))
                .thenAnswer(invocation -> {
                    AiStreamHandler handler = invocation.getArgument(1);
                    handler.onToken("partial");
                    return false;
                });

        streamingChatService.stream(42L, "user@example.com", request);

        verify(conversationService, never()).saveAssistantMessage(any(), any(), any());
    }

    @Test
    void shouldRegenerateExistingUserMessageWithoutSavingAnotherUserMessage() {
        ChatMessage userMessage = message(7L, MessageRole.USER, "Retry this response");
        ChatMessage assistantMessage = message(8L, MessageRole.ASSISTANT, "A regenerated response");
        AiChatRequest aiRequest = new AiChatRequest(List.of(
                new AiMessage(AiMessageRole.USER, userMessage.getContent())));

        when(conversationService.getUserMessage(42L, 7L, "user@example.com"))
                .thenReturn(userMessage);
        when(aiChatService.buildContextRequest(42L)).thenReturn(aiRequest);
        when(aiProvider.stream(eq(aiRequest), any(AiStreamHandler.class)))
                .thenAnswer(invocation -> {
                    AiStreamHandler handler = invocation.getArgument(1);
                    handler.onToken("A regenerated response");
                    return true;
                });
        when(conversationService.saveAssistantMessage(
                42L,
                "user@example.com",
                "A regenerated response"))
                .thenReturn(assistantMessage);
        when(conversationMapper.toMessageResponse(assistantMessage)).thenReturn(response(assistantMessage));

        streamingChatService.regenerate(42L, 7L, "user@example.com");

        verify(conversationService).getUserMessage(42L, 7L, "user@example.com");
        verify(conversationService, never()).saveUserMessage(any(), any(), any());
        verify(conversationService).saveAssistantMessage(
                42L,
                "user@example.com",
                "A regenerated response");
    }

    private CreateMessageRequest request(String content) {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent(content);
        return request;
    }

    private ChatMessage message(Long id, MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private ChatMessageResponse response(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}
