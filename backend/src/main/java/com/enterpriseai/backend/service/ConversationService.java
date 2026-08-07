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
import com.enterpriseai.backend.dto.UpdateConversationRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.Conversation;
import com.enterpriseai.backend.entity.MessageRole;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.repository.ChatMessageRepository;
import com.enterpriseai.backend.repository.ConversationRepository;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.repository.WorkspaceRepository;
import com.enterpriseai.backend.workspace.entity.Workspace;

@Service
public class ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationMapper conversationMapper;
    private final WorkspaceRepository workspaceRepository;

    public ConversationService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            ConversationMapper conversationMapper,
            WorkspaceRepository workspaceRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationMapper = conversationMapper;
        this.workspaceRepository = workspaceRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Conversation getConversationByIdAndWorkspace(Long conversationId) {
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        return conversationRepository.findByIdAndWorkspaceId(conversationId, context.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found in workspace"));
    }

    @Transactional
    public ConversationSummaryResponse createConversation(String currentUserEmail, CreateConversationRequest request) {
        User user = getUserByEmail(currentUserEmail);
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        Workspace workspace = workspaceRepository.getReferenceById(context.getWorkspaceId());

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = "New conversation";
        }

        Conversation conversation = new Conversation();
        conversation.setCreatedBy(user);
        conversation.setWorkspace(workspace);
        conversation.setTitle(title);
        
        if (request.getAgentId() != null && !request.getAgentId().isBlank()) {
            conversation.setAgentId(request.getAgentId());
        }
        
        conversation = conversationRepository.save(conversation);
        return conversationMapper.toSummaryResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationSummaryResponse> getConversations(String currentUserEmail, Pageable pageable) {
        WorkspaceContext context = WorkspaceContextHolder.getRequiredContext();
        return conversationRepository.findByWorkspaceIdOrderByUpdatedAtDesc(context.getWorkspaceId(), pageable)
                .map(conversationMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId, String currentUserEmail) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<ChatMessageResponse> messageResponses = conversationMapper.toMessageResponseList(messages);
                
        return conversationMapper.toConversationResponse(conversation, messageResponses);
    }

    @Transactional
    public ConversationSummaryResponse updateConversation(Long conversationId, String currentUserEmail, UpdateConversationRequest request) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
        if (request.getTitle() != null) {
            conversation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            conversation.setPinned(request.getPinned());
        }
        if (request.getFavorite() != null) {
            conversation.setFavorite(request.getFavorite());
        }
        if (request.getArchived() != null) {
            conversation.setArchived(request.getArchived());
        }
        
        conversation = conversationRepository.save(conversation);
        
        return conversationMapper.toSummaryResponse(conversation);
    }

    @Transactional
    public void deleteConversation(Long conversationId, String currentUserEmail) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
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
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
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
    public ChatMessageResponse editMessage(Long conversationId, Long messageId, String currentUserEmail, String newContent) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
        ChatMessage message = chatMessageRepository.findByIdAndConversationIdAndRole(
                        messageId,
                        conversation.getId(),
                        MessageRole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("User message not found"));
                
        message.setContent(newContent);
        message = chatMessageRepository.save(message);
        
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        return conversationMapper.toMessageResponse(message);
    }

    @Transactional
    public ChatMessage saveAssistantMessage(Long conversationId, String currentUserEmail, String content) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);

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
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);

        return chatMessageRepository.findByIdAndConversationIdAndRole(
                        messageId,
                        conversation.getId(),
                        MessageRole.USER)
                .orElseThrow(() -> new ResourceNotFoundException("User message not found"));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, String currentUserEmail) {
        Conversation conversation = getConversationByIdAndWorkspace(conversationId);
        
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        return conversationMapper.toMessageResponseList(messages);
    }
}
