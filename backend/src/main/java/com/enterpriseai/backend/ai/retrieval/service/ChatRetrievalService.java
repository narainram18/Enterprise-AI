package com.enterpriseai.backend.ai.retrieval.service;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.ai.config.RetrievalProperties;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalCitation;
import com.enterpriseai.backend.ai.retrieval.model.RetrievalStatistics;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;

@Service
public class ChatRetrievalService {

    private static final Logger log = Logger.getLogger(ChatRetrievalService.class.getName());

    private final UserRepository userRepository;
    private final SemanticSearchService semanticSearchService;
    private final RetrievalContextBuilder contextBuilder;
    private final RetrievalProperties properties;

    public ChatRetrievalService(
            UserRepository userRepository,
            SemanticSearchService semanticSearchService,
            RetrievalContextBuilder contextBuilder,
            RetrievalProperties properties) {
        this.userRepository = userRepository;
        this.semanticSearchService = semanticSearchService;
        this.contextBuilder = contextBuilder;
        this.properties = properties;
    }

    public ChatRetrievalResult retrieve(String query, String currentUserEmail) {
        if (!properties.enabled()) {
            return ChatRetrievalResult.empty(false);
        }

        try {
            User user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
            
            log.info("Executing vector search for query: " + query);
            var searchResults = semanticSearchService.search(query, user.getId());
            
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
