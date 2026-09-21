package com.enterpriseai.backend.ai.retrieval.hybrid;

import java.util.List;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;

public interface KeywordSearchService {
    List<RetrievedChunk> search(String query, Long workspaceId, Long userId);
}
