package com.enterpriseai.backend.dto;

import java.util.List;
import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.dto.DocumentResponse;

public record DashboardResponse(
    WorkspaceStats stats,
    List<Activity> activityFeed,
    List<DocumentResponse> recentDocuments,
    List<ConversationSummaryResponse> recentConversations,
    List<Agent> recentAgents,
    long totalChatMessages
) {}
