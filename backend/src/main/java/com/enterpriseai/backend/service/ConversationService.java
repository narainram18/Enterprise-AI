package com.enterpriseai.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.ConversationResponse;
import com.enterpriseai.backend.dto.ConversationSummaryResponse;
import com.enterpriseai.backend.dto.CreateConversationRequest;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.dto.RenameConversationRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.Conversation;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.repository.ChatMessageRepository;
import com.enterpriseai.backend.repository.ConversationRepository;
import com.enterpriseai.backend.repository.UserRepository;

@Service
public class ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationMapper conversationMapper;

    public ConversationService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            ConversationMapper conversationMapper) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationMapper = conversationMapper;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Conversation getConversationByIdAndUser(Long conversationId, User user) {
        return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    @Transactional
    public ConversationSummaryResponse createConversation(String currentUserEmail, CreateConversationRequest request) {
        User user = getUserByEmail(currentUserEmail);

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = "New conversation";
        }

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        
        conversation = conversationRepository.save(conversation);
        return conversationMapper.toSummaryResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationSummaryResponse> getConversations(String currentUserEmail, Pageable pageable) {
        User user = getUserByEmail(currentUserEmail);
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId(), pageable)
                .map(conversationMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<ChatMessageResponse> messageResponses = conversationMapper.toMessageResponseList(messages);
                
        return conversationMapper.toConversationResponse(conversation, messageResponses);
    }

    @Transactional
    public ConversationSummaryResponse renameConversation(Long conversationId, String currentUserEmail, RenameConversationRequest request) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        conversation.setTitle(request.getTitle());
        conversation = conversationRepository.save(conversation);
        
        return conversationMapper.toSummaryResponse(conversation);
    }

    @Transactional
    public void deleteConversation(Long conversationId, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        // Chat messages are automatically deleted via the database ON DELETE CASCADE constraint.
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ChatMessageResponse addUserMessage(Long conversationId, String currentUserEmail, CreateMessageRequest request) {
        return conversationMapper.toMessageResponse(
                saveUserMessage(conversationId, currentUserEmail, request));
    }

    @Transactional
    public ChatMessage saveUserMessage(Long conversationId, String currentUserEmail, CreateMessageRequest request) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(MessageRole.USER);
        message.setContent(request.getContent());
        
        message = chatMessageRepository.save(message);
        
        // Update conversation's updatedAt timestamp manually via the setter inherited from BaseEntity.
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        return message;
    }

    @Transactional
    public ChatMessage saveAssistantMessage(Long conversationId, String currentUserEmail, String content) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(MessageRole.ASSISTANT);
        message.setContent(content);

        message = chatMessageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return message;
    }

    @Transactional(readOnly = true)
    public ChatMessage getUserMessage(
            Long conversationId,
            Long messageId,
            String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);

        return chatMessageRepository.findByIdAndConversationIdAndRole(
                        messageId,
                        conversation.getId(),
                        MessageRole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("User message not found"));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        return conversationMapper.toMessageResponseList(messages);
    }
}
