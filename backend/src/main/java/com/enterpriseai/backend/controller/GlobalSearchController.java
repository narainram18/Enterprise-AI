package com.enterpriseai.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.util.*;
import java.util.stream.Collectors;
import com.enterpriseai.backend.dto.GlobalSearchResponse;
import com.enterpriseai.backend.dto.SearchResultItem;
import com.enterpriseai.backend.repository.ConversationRepository;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.entity.Conversation;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.agent.Agent;

@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {

    private final ConversationRepository conversationRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final AgentRegistry agentRegistry;
    private final RetrievalPipeline retrievalPipeline;
    private final com.enterpriseai.backend.repository.UserRepository userRepository;

    public GlobalSearchController(ConversationRepository conversationRepository,
                                  KnowledgeDocumentRepository documentRepository,
                                  AgentRegistry agentRegistry,
                                  RetrievalPipeline retrievalPipeline,
                                  com.enterpriseai.backend.repository.UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.documentRepository = documentRepository;
        this.agentRegistry = agentRegistry;
        this.retrievalPipeline = retrievalPipeline;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<GlobalSearchResponse> search(@RequestParam("q") String query,
                                                     @RequestParam(value = "filter", defaultValue = "All") String filter,
                                                     org.springframework.security.core.Authentication authentication) {
        Long workspaceId = WorkspaceContextHolder.getRequiredContext().getWorkspaceId();
        com.enterpriseai.backend.entity.User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Map<String, List<SearchResultItem>> resultsByCategory = new HashMap<>();
        int totalResults = 0;

        if ("All".equalsIgnoreCase(filter) || "Chats".equalsIgnoreCase(filter)) {
            Page<Conversation> chats = conversationRepository.findByWorkspaceIdAndTitleContainingIgnoreCase(workspaceId, query, PageRequest.of(0, 10));
            List<SearchResultItem> chatItems = chats.getContent().stream().map(c -> new SearchResultItem(
                    c.getId().toString(), "chat", c.getTitle(), "", "/app/chat/" + c.getId(), c.getUpdatedAt().toString(), "Title match"
            )).collect(Collectors.toList());
            resultsByCategory.put("Chats", chatItems);
            totalResults += chatItems.size();
        }

        if ("All".equalsIgnoreCase(filter) || "Documents".equalsIgnoreCase(filter)) {
            Page<KnowledgeDocument> docs = documentRepository.findByWorkspaceIdAndOriginalFileNameContainingIgnoreCase(workspaceId, query, PageRequest.of(0, 10));
            List<SearchResultItem> docItems = docs.getContent().stream().map(d -> new SearchResultItem(
                    d.getId().toString(), "document", d.getOriginalFileName(), d.getProcessingStatus().name(), "/app/documents", d.getCreatedAt().toString(), "Name match"
            )).collect(Collectors.toList());

            try {
                List<RetrievedChunk> chunks = retrievalPipeline.retrieveAndRank(query, workspaceId, user.getId());
                List<SearchResultItem> chunkItems = chunks.stream().limit(10).map(c -> new SearchResultItem(
                        c.documentId().toString(), "document_content", c.documentFileName(), c.chunkText(), "/app/documents", null, "Content match (Score: " + c.similarityScore() + ")"
                )).collect(Collectors.toList());
                docItems.addAll(chunkItems);
            } catch (Exception e) {}

            resultsByCategory.put("Documents", docItems);
            totalResults += docItems.size();
        }

        if ("All".equalsIgnoreCase(filter) || "Agents".equalsIgnoreCase(filter)) {
            List<SearchResultItem> agentItems = agentRegistry.getAllAgents().stream()
                    .filter(a -> a.name().toLowerCase().contains(query.toLowerCase()) || a.description().toLowerCase().contains(query.toLowerCase()))
                    .map(a -> new SearchResultItem(
                            a.id(), "agent", a.name(), a.description(), "/app/agents", null, "Agent match"
                    )).collect(Collectors.toList());
            resultsByCategory.put("Agents", agentItems);
            totalResults += agentItems.size();
        }

        return ResponseEntity.ok(new GlobalSearchResponse(query, resultsByCategory, totalResults));
    }
}
