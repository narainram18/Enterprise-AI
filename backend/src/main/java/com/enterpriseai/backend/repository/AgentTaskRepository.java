package com.enterpriseai.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.enterpriseai.backend.entity.AgentTask;
import com.enterpriseai.backend.workspace.entity.Workspace;

@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    Page<AgentTask> findByWorkspaceOrderByCreatedAtDesc(Workspace workspace, Pageable pageable);
}
