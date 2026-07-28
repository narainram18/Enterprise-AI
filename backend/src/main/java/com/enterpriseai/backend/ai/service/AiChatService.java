package com.enterpriseai.backend.ai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.context.ChatContextExtension;
import com.enterpriseai.backend.ai.exception.AiGenerationException;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.model.AiMessage;
import com.enterpriseai.backend.ai.model.AiMessageRole;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService;
import com.enterpriseai.backend.dto.AiChatTurnResponse;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.repository.ChatMessageRepository;
import com.enterpriseai.backend.service.ConversationService;

@Service
public class AiChatService {

    private final ConversationService conversationService;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationMapper conversationMapper;
    private final AiProvider aiProvider;
    private final AiChatProperties properties;
    private final ChatContextExtension chatContextExtension;
    private final ChatRetrievalService chatRetrievalService;

    public AiChatService(
            ConversationService conversationService,
            ChatMessageRepository chatMessageRepository,
            ConversationMapper conversationMapper,
            AiProvider aiProvider,
            AiChatProperties properties) {
        this(conversationService, chatMessageRepository, conversationMapper, aiProvider,
                properties, (historyRequest, retrievalContext) -> historyRequest, null);
    }

    @Autowired
    public AiChatService(
            ConversationService conversationService,
            ChatMessageRepository chatMessageRepository,
            ConversationMapper conversationMapper,
            AiProvider aiProvider,
            AiChatProperties properties,
            ChatContextExtension chatContextExtension,
            ChatRetrievalService chatRetrievalService) {
        this.conversationService = conversationService;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationMapper = conversationMapper;
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.chatContextExtension = chatContextExtension;
        this.chatRetrievalService = chatRetrievalService;
    }

    public AiChatService(
            ConversationService conversationService,
            ChatMessageRepository chatMessageRepository,
            ConversationMapper conversationMapper,
            AiProvider aiProvider,
            AiChatProperties properties,
            ChatContextExtension chatContextExtension) {
        this(conversationService, chatMessageRepository, conversationMapper, aiProvider,
                properties, chatContextExtension, null);
    }

    public AiChatTurnResponse chat(
            Long conversationId,
            String currentUserEmail,
            CreateMessageRequest request) {

        ChatMessage userMessage = conversationService.saveUserMessage(
                conversationId,
                currentUserEmail,
                request);

        ChatRetrievalResult retrieval = retrieve(request.getContent(), currentUserEmail);
        AiChatRequest aiRequest = buildContextRequest(conversationId, retrieval.context());

        String assistantContent = generateResponse(aiRequest);

        ChatMessage assistantMessage = conversationService.saveAssistantMessage(
                conversationId,
                currentUserEmail,
                assistantContent);

        ChatMessageResponse userResponse = conversationMapper.toMessageResponse(userMessage);
        ChatMessageResponse assistantResponse = conversationMapper.toMessageResponse(assistantMessage);

        return new AiChatTurnResponse(
                userResponse,
                assistantResponse,
                retrieval.statistics().retrievalAttempted() ? retrieval.citations() : null,
                retrieval.statistics().retrievalAttempted() ? retrieval.statistics() : null);
    }

    AiChatRequest buildContextRequest(Long conversationId) {
        List<ChatMessage> contextMessages = loadRecentContext(conversationId);
        return toAiChatRequest(contextMessages);
    }

    AiChatRequest buildContextRequest(Long conversationId, String retrievalContext) {
        return chatContextExtension.extend(buildContextRequest(conversationId), retrievalContext);
    }

    ChatRetrievalResult retrieve(String query, String currentUserEmail) {
        return chatRetrievalService == null
                ? ChatRetrievalResult.empty(false)
                : chatRetrievalService.retrieve(query, currentUserEmail);
    }

    private List<ChatMessage> loadRecentContext(Long conversationId) {
        Pageable pageable = PageRequest.of(
                0,
                properties.maxContextMessages(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")));

        List<ChatMessage> recentMessages = new ArrayList<>(
                chatMessageRepository.findByConversationId(conversationId, pageable));
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    private AiChatRequest toAiChatRequest(List<ChatMessage> messages) {
        List<AiMessage> aiMessages = messages.stream()
                .map(message -> new AiMessage(
                        toAiMessageRole(message.getRole()),
                        message.getContent()))
                .toList();

        return new AiChatRequest(aiMessages);
    }

    private String generateResponse(AiChatRequest request) {
        try {
            String response = aiProvider.generate(request);
            if (response == null || response.isBlank()) {
                throw new AiGenerationException(
                        "AI provider returned an invalid response");
            }
            return response;
        } catch (AiGenerationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiGenerationException(
                    "AI generation failed",
                    ex);
        }
    }

    private AiMessageRole toAiMessageRole(MessageRole role) {
        return switch (role) {
            case SYSTEM -> AiMessageRole.SYSTEM;
            case USER -> AiMessageRole.USER;
            case ASSISTANT -> AiMessageRole.ASSISTANT;
        };
    }
}
