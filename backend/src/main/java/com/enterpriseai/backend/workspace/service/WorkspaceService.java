package com.enterpriseai.backend.workspace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.exception.ForbiddenException;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.workspace.entity.Workspace;
import com.enterpriseai.backend.workspace.entity.WorkspaceMember;
import com.enterpriseai.backend.workspace.entity.WorkspaceRole;
import com.enterpriseai.backend.workspace.repository.WorkspaceRepository;
import com.enterpriseai.backend.workspace.repository.WorkspaceMemberRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository, WorkspaceMemberRepository workspaceMemberRepository, UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Workspace> listWorkspaces(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return workspaceRepository.findAllByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspace(Long id, String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ForbiddenException("Not a member of this workspace"));
        return member.getWorkspace();
    }

    @Transactional
    public Workspace createWorkspace(String name, String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setOwner(user);
        workspace.setCreatedAt(LocalDateTime.now());
        workspace.setUpdatedAt(LocalDateTime.now());
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        member.setJoinedAt(LocalDateTime.now());
        workspaceMemberRepository.save(member);

        return workspace;
    }

    @Transactional
    public Workspace renameWorkspace(Long id, String newName, String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ForbiddenException("Not a member of this workspace"));
        
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.ADMIN) {
            throw new ForbiddenException("Only OWNER or ADMIN can rename a workspace");
        }

        Workspace workspace = member.getWorkspace();
        workspace.setName(newName);
        workspace.setUpdatedAt(LocalDateTime.now());
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void deleteWorkspace(Long id, String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ForbiddenException("Not a member of this workspace"));
        
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only OWNER can delete a workspace");
        }
        
        workspaceRepository.delete(member.getWorkspace());
    }
}
