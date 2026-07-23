package com.enterpriseai.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.MessageRole;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<ChatMessage> findByConversationId(Long conversationId, Pageable pageable);

    java.util.Optional<ChatMessage> findByIdAndConversationIdAndRole(
            Long id,
            Long conversationId,
            MessageRole role);
}
