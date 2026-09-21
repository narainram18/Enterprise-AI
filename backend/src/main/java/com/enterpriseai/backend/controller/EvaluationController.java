package com.enterpriseai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.ai.retrieval.hybrid.RetrievalPipeline;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.repository.UserRepository;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final RetrievalPipeline retrievalPipeline;
    private final UserRepository userRepository;

    public EvaluationController(RetrievalPipeline retrievalPipeline, UserRepository userRepository) {
        this.retrievalPipeline = retrievalPipeline;
        this.userRepository = userRepository;
    }

    @GetMapping("/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runEvaluation(Authentication authentication) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Long workspaceId = WorkspaceContextHolder.getRequiredContext().getWorkspaceId();

        // Run a suite of predefined automated RAG tests on the live index
        List<String> testQueries = List.of(
            "What is the company policy?",
            "How does RAG work?",
            "Who is the CEO?",
            "Duplicate test query"
        );

        int totalQueries = testQueries.size();
        int successfulRetrievals = 0;
        long totalLatencyMs = 0;

        for (String query : testQueries) {
            long start = System.currentTimeMillis();
            var chunks = retrievalPipeline.retrieveAndRank(query, workspaceId, user.getId());
            totalLatencyMs += (System.currentTimeMillis() - start);

            if (!chunks.isEmpty()) {
                successfulRetrievals++;
            }
        }

        Map<String, Object> results = Map.of(
            "totalTests", totalQueries,
            "successfulRetrievals", successfulRetrievals,
            "retrievalSuccessRate", (double) successfulRetrievals / totalQueries,
            "averageLatencyMs", totalQueries > 0 ? totalLatencyMs / totalQueries : 0,
            "evaluationMethod", "Automated Retrieval Tests on Active Workspace Index"
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Evaluation completed", results));
    }
}
