package com.enterpriseai.backend.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.enterpriseai.backend.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    
    @Query("SELECT w FROM Workspace w JOIN WorkspaceMember wm ON w.id = wm.workspace.id WHERE wm.user.id = :userId")
    List<Workspace> findAllByUserId(@Param("userId") Long userId);
}
