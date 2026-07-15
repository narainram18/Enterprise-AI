package com.enterpriseai.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.enterpriseai.backend.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}
