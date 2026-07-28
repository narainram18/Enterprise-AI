package com.enterpriseai.backend.ai.provider;

import java.util.List;

public interface EmbeddingProvider {

    List<Double> embed(String text);

    List<List<Double>> embedBatch(List<String> texts);
}
