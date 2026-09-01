package com.enterpriseai.backend.ai.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.context.DefaultChatContextExtension;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;
import com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.repository.ChatMessageRepository;
import com.enterpriseai.backend.service.ConversationService;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.entity.Conversation;

@ExtendWith(MockitoExtension.class)
class AiStreamingRagIntegrationTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private AiProvider aiProvider;
    @Mock
    private ChatRetrievalService retrievalService;
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private com.enterpriseai.backend.ai.tool.ToolExecutor toolExecutor;

    @Test
    void streamsWithRetrievedContextWithoutChangingProviderStreamingContract() {
        com.enterpriseai.backend.workspace.context.WorkspaceContextHolder.setContext(
                new com.enterpriseai.backend.workspace.context.WorkspaceContext(7L, com.enterpriseai.backend.workspace.entity.WorkspaceRole.OWNER, true));
        try {
        AiChatService aiChatService = new AiChatService(
                conversationService,
                chatMessageRepository,
                conversationMapper,
                aiProvider,
                new AiChatProperties(10, 120000, 1000, 2000, 4000),
                new DefaultChatContextExtension(new RagPromptBuilder(new TokenBudgetManager(), new AiChatProperties(10, 120000, 1000, 2000, 4000)), new com.enterpriseai.backend.ai.tool.ToolRegistry(java.util.List.of())),
                retrievalService,
                agentRegistry);
        AiStreamingChatService service = new AiStreamingChatService(
                conversationService,
                aiChatService,
                conversationMapper,
                aiProvider,
                new AiChatProperties(10, 120000, 1000, 2000, 4000),
                Runnable::run,
                retrievalService,
                agentRegistry,
                toolExecutor,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("What are my internship responsibilities?");
        ChatMessage user = message(1L, MessageRole.USER, request.getContent());
        ChatMessage assistant = message(2L, MessageRole.ASSISTANT, "You own onboarding.");
        ChatMessageResponse userResponse = new ChatMessageResponse(1L, MessageRole.USER, request.getContent(), user.getCreatedAt());
        ChatMessageResponse assistantResponse = new ChatMessageResponse(2L, MessageRole.ASSISTANT, assistant.getContent(), assistant.getCreatedAt());
        when(conversationService.saveUserMessage(42L, "user@example.com", request)).thenReturn(user);
        when(chatMessageRepository.findByConversationId(eq(42L), any(org.springframework.data.domain.Pageable.class))).thenReturn(List.of(user));
        when(agentRegistry.getAgent("general-assistant")).thenReturn(new Agent("general-assistant", "General", "Desc", "Icon", "Color", "Prompt", 0.7, 0.9, "Model", true, true, java.util.List.of(), null, null, java.util.List.of()));
        when(conversationMapper.toMessageResponse(user)).thenReturn(userResponse);
        when(conversationMapper.toMessageResponse(assistant)).thenReturn(assistantResponse);
        when(retrievalService.retrieve(request.getContent(), "user@example.com"))
                .thenReturn(new ChatRetrievalResult(
                        "[Document: internship.pdf]\n\nContent:\nOwn onboarding",
                        List.of(new RetrievalCitation(9L, "internship.pdf", 0, null, 0.9)),
                        new RetrievalStatistics(1, 1, true)));
        when(aiProvider.stream(any(AiChatRequest.class), any())).thenAnswer(invocation -> {
            invocation.<com.enterpriseai.backend.ai.provider.AiStreamHandler>getArgument(1).onToken("grounded");
            return true;
        });
        when(conversationService.saveAssistantMessage(eq(42L), eq("user@example.com"), eq("grounded"), any(), any()))
                .thenReturn(assistant);

        service.stream(42L, "user@example.com", request);

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiProvider).stream(requestCaptor.capture(), any());
        System.out.println("DEBUG STREAMING REQUEST:\n" + requestCaptor.getValue().messages().get(1).content());
        assertTrue(requestCaptor.getValue().messages().get(1).content().contains("internship.pdf"));
        assertTrue(requestCaptor.getValue().messages().get(1).content().contains("What are my internship responsibilities?"));
        } finally {
            com.enterpriseai.backend.workspace.context.WorkspaceContextHolder.clearContext();
        }
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
}


