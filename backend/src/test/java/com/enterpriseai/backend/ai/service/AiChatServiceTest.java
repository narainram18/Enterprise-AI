package com.enterpriseai.backend.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.context.DefaultChatContextExtension;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.exception.AiGenerationException;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.dto.AiChatTurnResponse;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.service.ConversationService;
import com.enterpriseai.backend.repository.ChatMessageRepository;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.entity.Conversation;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private AgentRegistry agentRegistry;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService(
                conversationService,
                chatMessageRepository,
                conversationMapper,
                aiProvider,
                new AiChatProperties(2, 120000, 1000, 2000, 4000),
                new DefaultChatContextExtension(new RagPromptBuilder(new TokenBudgetManager(), new AiChatProperties(10, 120000, 1000, 2000, 4000)), new com.enterpriseai.backend.ai.tool.ToolRegistry(java.util.List.of())),
                agentRegistry);
    }

    @Test
    void shouldPersistUserMessageGenerateResponseAndPersistAssistantMessage() {
        CreateMessageRequest request = request("What is the status?");
        ChatMessage userMessage = message(1L, MessageRole.USER, request.getContent());
        ChatMessage assistantMessage = message(2L, MessageRole.ASSISTANT, "Everything is operational.");
        ChatMessageResponse userResponse = response(userMessage);
        ChatMessageResponse assistantResponse = response(assistantMessage);

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(userMessage);
        when(chatMessageRepository.findByConversationId(eq(42L), any(Pageable.class)))
                .thenReturn(List.of(userMessage));
        when(agentRegistry.getAgent("general-assistant")).thenReturn(new Agent("general-assistant", "General", "Desc", "Icon", "Color", "Prompt", 0.7, 0.9, "Model", false, true, java.util.List.of(), null, null, java.util.List.of()));
        when(aiProvider.generate(any(AiChatRequest.class)))
                .thenReturn(assistantMessage.getContent());
        when(conversationService.saveAssistantMessage(
                eq(42L),
                eq("user@example.com"),
                eq(assistantMessage.getContent()),
                any(),
                any()))
                .thenReturn(assistantMessage);
        when(conversationMapper.toMessageResponse(userMessage)).thenReturn(userResponse);
        when(conversationMapper.toMessageResponse(assistantMessage)).thenReturn(assistantResponse);

        AiChatTurnResponse result = aiChatService.chat(42L, "user@example.com", request);

        assertSame(userResponse, result.userMessage());
        assertSame(assistantResponse, result.assistantMessage());

        InOrder order = inOrder(conversationService, chatMessageRepository, aiProvider);
        order.verify(conversationService).saveUserMessage(42L, "user@example.com", request);
        order.verify(chatMessageRepository).findByConversationId(eq(42L), any(Pageable.class));
        order.verify(aiProvider).generate(any(AiChatRequest.class));
        order.verify(conversationService).saveAssistantMessage(
                eq(42L),
                eq("user@example.com"),
                eq(assistantMessage.getContent()),
                any(),
                any());
    }

    @Test
    void shouldLimitContextAndSendMessagesInChronologicalOrderIncludingNewestUserMessage() {
        CreateMessageRequest request = request("Latest question");
        ChatMessage olderAssistant = message(10L, MessageRole.ASSISTANT, "Earlier answer");
        ChatMessage newestUser = message(11L, MessageRole.USER, request.getContent());
        ChatMessage assistantMessage = message(12L, MessageRole.ASSISTANT, "Latest answer");

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(newestUser);
        when(chatMessageRepository.findByConversationId(eq(42L), any(Pageable.class)))
                .thenReturn(List.of(newestUser, olderAssistant));
        when(agentRegistry.getAgent("general-assistant")).thenReturn(new Agent("general-assistant", "General", "Desc", "Icon", "Color", "Prompt", 0.7, 0.9, "Model", false, true, java.util.List.of(), null, null, java.util.List.of()));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(assistantMessage.getContent());
        when(conversationService.saveAssistantMessage(
                eq(42L),
                eq("user@example.com"),
                eq(assistantMessage.getContent()),
                any(),
                any()))
                .thenReturn(assistantMessage);

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        aiChatService.chat(42L, "user@example.com", request);

        verify(aiProvider).generate(requestCaptor.capture());
        assertEquals(
                List.of(
                        new AiMessage(AiMessageRole.ASSISTANT, olderAssistant.getContent()),
                        new AiMessage(AiMessageRole.USER, newestUser.getContent())),
                requestCaptor.getValue().messages());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository).findByConversationId(eq(42L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageSize());
        assertEquals("DESC", pageable.getSort().getOrderFor("createdAt").getDirection().name());
        assertEquals("DESC", pageable.getSort().getOrderFor("id").getDirection().name());
    }

    @Test
    void shouldNotCallProviderWhenConversationOwnershipValidationFails() {
        CreateMessageRequest request = request("Unauthorized question");
        when(conversationService.saveUserMessage(42L, "other@example.com", request))
                .thenThrow(new ResourceNotFoundException("Conversation not found"));

        assertThrows(
                ResourceNotFoundException.class,
                () -> aiChatService.chat(42L, "other@example.com", request));

        verifyNoInteractions(chatMessageRepository, aiProvider);
        verify(conversationService, never()).saveAssistantMessage(any(), any(), any(), any(), any());
    }

    @Test
    void shouldKeepUserMessageAndNotPersistAssistantWhenProviderFails() {
        CreateMessageRequest request = request("This will fail");
        ChatMessage userMessage = message(1L, MessageRole.USER, request.getContent());

        when(conversationService.saveUserMessage(42L, "user@example.com", request))
                .thenReturn(userMessage);
        when(chatMessageRepository.findByConversationId(eq(42L), any(Pageable.class)))
                .thenReturn(List.of(userMessage));
        when(agentRegistry.getAgent("general-assistant")).thenReturn(new Agent("general-assistant", "General", "Desc", "Icon", "Color", "Prompt", 0.7, 0.9, "Model", false, true, java.util.List.of(), null, null, java.util.List.of()));
        when(aiProvider.generate(any(AiChatRequest.class)))
                .thenThrow(new AiGenerationException("AI provider is unavailable"));

        assertThrows(
                AiGenerationException.class,
                () -> aiChatService.chat(42L, "user@example.com", request));

        verify(conversationService).saveUserMessage(42L, "user@example.com", request);
        verify(conversationService, never()).saveAssistantMessage(any(), any(), any(), any(), any());
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
        Conversation conversation = new Conversation();
        conversation.setAgentId("general-assistant");
        message.setConversation(conversation);
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


