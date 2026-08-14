package com.enterpriseai.backend.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.data.domain.Pageable;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.context.DefaultChatContextExtension;
import com.enterpriseai.backend.ai.context.RagPromptBuilder;
import com.enterpriseai.backend.ai.context.TokenBudgetManager;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;
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
class AiChatRagIntegrationTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private AiProvider aiProvider;
    @Mock
    private com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService retrievalService;
    @Mock
    private AgentRegistry agentRegistry;

    @Test
    void augmentsProviderRequestAndReturnsCitations() {
        AiChatService service = new AiChatService(
                conversationService,
                chatMessageRepository,
                conversationMapper,
                aiProvider,
                new AiChatProperties(10, 120000, 1000, 2000, 4000),
                new DefaultChatContextExtension(new RagPromptBuilder(new TokenBudgetManager(), new AiChatProperties(10, 120000, 1000, 2000, 4000)), new com.enterpriseai.backend.ai.tool.ToolRegistry(java.util.List.of())),
                retrievalService,
                agentRegistry);

        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("What are my responsibilities?");
        ChatMessage user = message(1L, MessageRole.USER, request.getContent());
        Conversation conversation = new Conversation();
        conversation.setAgentId("general-assistant");
        user.setConversation(conversation);
        ChatMessage assistant = message(2L, MessageRole.ASSISTANT, "You own onboarding.");
        when(conversationService.saveUserMessage(42L, "user@example.com", request)).thenReturn(user);
        when(chatMessageRepository.findByConversationId(eq(42L), any(Pageable.class))).thenReturn(List.of(user));
        when(agentRegistry.getAgent("general-assistant")).thenReturn(new Agent("general-assistant", "General", "Desc", "Icon", "Color", "Prompt", 0.7, 0.9, "Model", true, true, java.util.List.of(), null, null, java.util.List.of()));
        when(retrievalService.retrieve(request.getContent(), "user@example.com"))
                .thenReturn(new ChatRetrievalResult(
                        "[Document: handbook.pdf]\n\nContent:\nOwn onboarding",
                        List.of(new RetrievalCitation(9L, "handbook.pdf", 2, null, 0.92)),
                        new RetrievalStatistics(1, 1, true)));
        when(aiProvider.generate(any(AiChatRequest.class))).thenReturn(assistant.getContent());
        when(conversationService.saveAssistantMessage(
                eq(42L),
                eq("user@example.com"),
                eq(assistant.getContent()),
                any(),
                any()))
                .thenReturn(assistant);

        var result = service.chat(42L, "user@example.com", request);

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiProvider).generate(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().messages().get(1).content().contains("Retrieved Knowledge"));
        assertTrue(requestCaptor.getValue().messages().get(1).content().contains("Own onboarding"));
        assertEquals(1, result.citations().size());
        assertEquals(1, result.retrievalStatistics().retrievedChunks());
    }

    private ChatMessage message(Long id, MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}


