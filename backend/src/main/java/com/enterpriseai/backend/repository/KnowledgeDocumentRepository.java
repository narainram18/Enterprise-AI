package com.enterpriseai.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.enterpriseai.backend.entity.KnowledgeDocument;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long>, JpaSpecificationExecutor<KnowledgeDocument> {

    Page<KnowledgeDocument> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndWorkspaceId(Long id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM KnowledgeDocument d WHERE d.workspace.id = :workspaceId")
    long sumFileSizeByWorkspaceId(@org.springframework.data.repository.query.Param("workspaceId") Long workspaceId);

    Page<KnowledgeDocument> findByWorkspaceIdAndOriginalFileNameContainingIgnoreCase(Long workspaceId, String fileName, Pageable pageable);

    boolean existsByWorkspaceIdAndOriginalFileName(Long workspaceId, String originalFileName);
}
