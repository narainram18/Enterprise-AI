package com.enterpriseai.backend.ai.retrieval.service;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;

import org.springframework.cache.annotation.Cacheable;

@Service
public class ChatRetrievalService {

    private static final Logger log = Logger.getLogger(ChatRetrievalService.class.getName());

    private final UserRepository userRepository;
    private final RetrievalPipeline retrievalPipeline;
    private final RetrievalContextBuilder contextBuilder;
    private final RetrievalProperties properties;

    public ChatRetrievalService(
            UserRepository userRepository,
            RetrievalPipeline retrievalPipeline,
            RetrievalContextBuilder contextBuilder,
            RetrievalProperties properties) {
        this.userRepository = userRepository;
        this.retrievalPipeline = retrievalPipeline;
        this.contextBuilder = contextBuilder;
        this.properties = properties;
    }

    @Cacheable(value = "retrievalResults", unless = "#result == null || #result.context().isEmpty()")
    public ChatRetrievalResult retrieve(String query, String currentUserEmail) {
        log.info("CACHE MISS - Executing retrieval");
        if (!properties.enabled()) {
            return ChatRetrievalResult.empty(false);
        }

        try {
            WorkspaceContext workspaceContext = WorkspaceContextHolder.getContext();
            
            log.info("ChatRetrievalService - CACHE MISS - Executing hybrid retrieval for query: '" + query + "'");
            var searchResults = retrievalPipeline.retrieveAndRank(query, workspaceContext.getWorkspaceId());
            
            List<RetrievedChunk> chunks = contextBuilder.select(searchResults);
            log.info("Number of retrieved chunks after selection: " + chunks.size());
            for (RetrievedChunk chunk : chunks) {
                log.info("Retrieved chunk similarity score: " + chunk.similarityScore());
            }
            
            String context = contextBuilder.build(chunks);
            log.info("Generated retrieval context length: " + context.length());
            
            List<RetrievalCitation> citations = chunks.stream()
                    .map(chunk -> new RetrievalCitation(
                            chunk.documentId(),
                            chunk.documentFileName(),
                            chunk.chunkIndex(),
                            chunk.similarityScore()))
                    .toList();
            int documents = (int) chunks.stream()
                    .map(RetrievedChunk::documentId)
                    .distinct()
                    .count();
            return new ChatRetrievalResult(
                    context,
                    citations,
                    new RetrievalStatistics(chunks.size(), documents, true));
        } catch (RuntimeException ex) {
            log.warning(() -> "Semantic retrieval skipped: " + ex.getMessage());
            return ChatRetrievalResult.empty(true);
        }
    }
}
