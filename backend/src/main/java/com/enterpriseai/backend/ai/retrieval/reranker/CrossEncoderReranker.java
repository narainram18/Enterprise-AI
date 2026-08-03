package com.enterpriseai.backend.ai.retrieval.reranker;

import java.util.List;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;

public interface CrossEncoderReranker {
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> chunks);
}
