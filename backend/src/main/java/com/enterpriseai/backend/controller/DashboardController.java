package com.enterpriseai.backend.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.dto.Activity;
import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.dto.ConversationSummaryResponse;
import com.enterpriseai.backend.dto.DashboardResponse;
import com.enterpriseai.backend.dto.WorkspaceStats;
import com.enterpriseai.backend.dto.DocumentResponse;
import com.enterpriseai.backend.entity.Conversation;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.repository.ConversationRepository;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.workspace.repository.WorkspaceMemberRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ConversationRepository conversationRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AgentRegistry agentRegistry;

    public DashboardController(
            ConversationRepository conversationRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            AgentRegistry agentRegistry) {
        this.conversationRepository = conversationRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.agentRegistry = agentRegistry;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        Long workspaceId = WorkspaceContextHolder.getRequiredContext().getWorkspaceId();

        long conversationsCount = conversationRepository.countByWorkspaceId(workspaceId);
        long documentsCount = knowledgeDocumentRepository.countByWorkspaceId(workspaceId);
        long agentsCount = agentRegistry.getAllAgents().size();
        long membersCount = workspaceMemberRepository.countByWorkspaceId(workspaceId);
        long storageUsedBytes = knowledgeDocumentRepository.sumFileSizeByWorkspaceId(workspaceId);

        WorkspaceStats stats = new WorkspaceStats(
                conversationsCount, documentsCount, agentsCount, membersCount, storageUsedBytes
        );

        List<Conversation> recentConvs = conversationRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, PageRequest.of(0, 5)).getContent();
        List<KnowledgeDocument> recentDocs = knowledgeDocumentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(0, 5)).getContent();

        List<ConversationSummaryResponse> convSummaries = recentConvs.stream()
                .map(c -> new ConversationSummaryResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(), c.getAgentId(), c.isPinned(), c.isFavorite(), c.isArchived()))
                .toList();
        
        List<DocumentResponse> docResponses = recentDocs.stream()
                .map(d -> new DocumentResponse(
                        d.getId(), d.getOriginalFileName(), d.getContentType(), d.getFileSize(), d.getVersion(),
                        d.getDocumentType(), d.getProcessingStatus(), d.getExtractionError(),
                        d.getCreatedAt(), d.getUpdatedAt()
                )).toList();

        List<Activity> activityFeed = new ArrayList<>();
        recentConvs.forEach(c -> activityFeed.add(new Activity("conv_" + c.getId(), "CONVERSATION", c.getTitle(), "Started a new conversation", c.getUpdatedAt(), "/app/chat/" + c.getId())));
        recentDocs.forEach(d -> activityFeed.add(new Activity("doc_" + d.getId(), "DOCUMENT", d.getOriginalFileName(), "Uploaded document", d.getCreatedAt(), "/app/documents")));
        activityFeed.sort(Comparator.comparing(Activity::timestamp).reversed());

        List<String> recentAgentIds = recentConvs.stream()
                .map(Conversation::getAgentId)
                .filter(id -> id != null)
                .distinct()
                .limit(4)
                .toList();
        
        List<Agent> recentAgents = recentAgentIds.stream()
                .map(id -> agentRegistry.getAgent(id))
                .filter(a -> a != null)
                .toList();

        if (recentAgents.isEmpty()) {
             recentAgents = agentRegistry.getAllAgents().stream().limit(4).toList();
        }

        DashboardResponse response = new DashboardResponse(
                stats,
                activityFeed.stream().limit(10).toList(),
                docResponses,
                convSummaries,
                recentAgents,
                0
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard fetched successfully", response));
    }
}
