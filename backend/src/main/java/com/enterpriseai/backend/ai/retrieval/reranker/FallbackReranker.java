package com.enterpriseai.backend.ai.retrieval.reranker;

import java.util.List;
import org.springframework.stereotype.Service;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FallbackReranker implements CrossEncoderReranker {

    private static final Logger log = LoggerFactory.getLogger(FallbackReranker.class);

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> chunks) {
        log.info("FallbackReranker: Skipping cross-encoder reranking, returning {} chunks as-is", chunks.size());
        // For now, return the chunks unmodified (rely on RRF scoring)
        // In the future, this will call Ollama cross-encoder models
        return chunks;
    }
}
