package com.enterpriseai.backend.ai.retrieval.model;

import java.util.List;

public record ChatRetrievalResult(
        String context,
        List<RetrievalCitation> citations,
        RetrievalStatistics statistics
) {
    public ChatRetrievalResult {
        context = context == null ? "" : context;
        citations = citations == null ? List.of() : List.copyOf(citations);
        statistics = statistics == null ? RetrievalStatistics.notAttempted() : statistics;
    }

    public static ChatRetrievalResult empty(boolean attempted) {
        return new ChatRetrievalResult(
                "",
                List.of(),
                new RetrievalStatistics(0, 0, attempted));
    }
}
