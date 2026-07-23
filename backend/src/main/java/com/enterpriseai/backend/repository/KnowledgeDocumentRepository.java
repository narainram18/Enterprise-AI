package com.enterpriseai.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.enterpriseai.backend.entity.KnowledgeDocument;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Page<KnowledgeDocument> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndUserId(Long id, Long userId);
}
