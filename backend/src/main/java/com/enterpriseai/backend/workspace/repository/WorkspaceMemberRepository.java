package com.enterpriseai.backend.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.enterpriseai.backend.workspace.entity.WorkspaceMember;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"workspace"})
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    long countByWorkspaceId(Long workspaceId);
}
