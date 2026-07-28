package com.enterpriseai.backend.ai.retrieval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.ai.config.RetrievalProperties;

class RetrievalPropertiesTest {

    @Test
    void invalidValuesFallBackToSafeDefaults() {
        RetrievalProperties properties = new RetrievalProperties(0, 2.0, 0, 0);

        assertEquals(10, properties.topK());
        assertEquals(0.5, properties.minimumSimilarityScore());
        assertEquals(5, properties.maximumRetrievedChunks());
        assertEquals(5000, properties.searchTimeout());
    }
}
