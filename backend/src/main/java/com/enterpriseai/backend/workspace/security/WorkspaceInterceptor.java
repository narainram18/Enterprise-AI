package com.enterpriseai.backend.workspace.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.entity.WorkspaceMember;
import com.enterpriseai.backend.workspace.repository.WorkspaceMemberRepository;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class WorkspaceInterceptor implements HandlerInterceptor {

    private static final String WORKSPACE_HEADER = "X-Workspace-Id";
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public WorkspaceInterceptor(WorkspaceMemberRepository workspaceMemberRepository, UserRepository userRepository) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String workspaceIdHeader = request.getHeader(WORKSPACE_HEADER);
        if (workspaceIdHeader == null || workspaceIdHeader.isEmpty()) {
            return true; // Optional header for endpoints that don't need a workspace (e.g. auth, listing workspaces)
        }

        Long workspaceId;
        try {
            workspaceId = Long.parseLong(workspaceIdHeader);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Workspace ID format");
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return true;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return true;
        }

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElse(null);

        if (member == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied to this Workspace");
            return false;
        }

        WorkspaceContext context = new WorkspaceContext(workspaceId, member.getRole());
        WorkspaceContextHolder.setContext(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        WorkspaceContextHolder.clearContext();
    }
}
