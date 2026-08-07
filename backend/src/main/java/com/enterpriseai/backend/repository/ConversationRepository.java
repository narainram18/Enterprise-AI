package com.enterpriseai.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.enterpriseai.backend.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId, Pageable pageable);

    Optional<Conversation> findByIdAndWorkspaceId(Long id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    Page<Conversation> findByWorkspaceIdAndTitleContainingIgnoreCase(Long workspaceId, String title, Pageable pageable);
}
