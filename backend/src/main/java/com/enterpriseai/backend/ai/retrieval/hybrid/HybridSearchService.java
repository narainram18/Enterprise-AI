package com.enterpriseai.backend.ai.retrieval.hybrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.ai.retrieval.service.SemanticSearchService;
import com.enterpriseai.backend.ai.retrieval.reranker.CrossEncoderReranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HybridSearchService implements RetrievalPipeline {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int RRF_K = 60;

    private final SemanticSearchService vectorSearchService;
    private final KeywordSearchService keywordSearchService;
    private final CrossEncoderReranker reranker;

    public HybridSearchService(
            SemanticSearchService vectorSearchService,
            KeywordSearchService keywordSearchService,
            CrossEncoderReranker reranker) {
        this.vectorSearchService = vectorSearchService;
        this.keywordSearchService = keywordSearchService;
        this.reranker = reranker;
    }

    @Override
    public List<RetrievedChunk> retrieveAndRank(String query, Long workspaceId) {
        log.info("HybridSearchService: Executing hybrid search for query '{}'", query);

        // 1. Execute both searches (in sequence for simplicity, could be parallelized)
        List<RetrievedChunk> vectorResults = vectorSearchService.search(query, workspaceId);
        log.info("--- SEMANTIC SEARCH RESULTS ---");
        for (RetrievedChunk c : vectorResults) {
            log.info("Chunk ID: {}, Score: {}", c.chunkId(), c.similarityScore());
        }

        List<RetrievedChunk> keywordResults = keywordSearchService.search(query, workspaceId);
        log.info("--- KEYWORD SEARCH RESULTS ---");
        for (RetrievedChunk c : keywordResults) {
            log.info("Chunk ID: {}, Score: {}", c.chunkId(), c.similarityScore());
        }

        // 2. Combine and score using Reciprocal Rank Fusion (RRF)
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, RetrievedChunk> chunkMap = new HashMap<>();

        // Score vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            RetrievedChunk chunk = vectorResults.get(i);
            double score = 1.0 / (RRF_K + i + 1);
            rrfScores.put(chunk.chunkId(), rrfScores.getOrDefault(chunk.chunkId(), 0.0) + score);
            chunkMap.put(chunk.chunkId(), chunk);
        }

        // Score keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            RetrievedChunk chunk = keywordResults.get(i);
            double score = 1.0 / (RRF_K + i + 1);
            rrfScores.put(chunk.chunkId(), rrfScores.getOrDefault(chunk.chunkId(), 0.0) + score);
            chunkMap.put(chunk.chunkId(), chunk);
        }

        // 3. Sort by RRF score
        List<RetrievedChunk> fusedResults = new ArrayList<>(chunkMap.values());
        fusedResults.sort(Comparator.comparing((RetrievedChunk c) -> rrfScores.get(c.chunkId())).reversed());

        // Limit to top 20 before reranking
        int limit = Math.min(20, fusedResults.size());
        fusedResults = fusedResults.subList(0, limit);

        // Update similarity score in the returned chunks to reflect RRF score
        List<RetrievedChunk> rrfRankedChunks = fusedResults.stream().map(c -> 
            new RetrievedChunk(
                c.documentId(), c.chunkId(), c.chunkIndex(),
                rrfScores.get(c.chunkId()), // Use RRF score
                c.documentFileName(), c.documentType(), c.chunkText(), c.workspaceId()
            )
        ).toList();

        // 4. Cross-Encoder Reranking
        List<RetrievedChunk> finalResults = reranker.rerank(query, rrfRankedChunks);

        log.info("--- HYBRID RESULTS (RRF + Reranker) ---");
        for (int i = 0; i < finalResults.size(); i++) {
            RetrievedChunk c = finalResults.get(i);
            log.info("Rank: {}, Chunk ID: {}, Final Score: {}", i + 1, c.chunkId(), c.similarityScore());
        }

        // Limit to top 5 final results
        int finalLimit = Math.min(5, finalResults.size());
        return finalResults.subList(0, finalLimit);
    }
}
