package com.enterpriseai.backend.ai.vector.qdrant;

import com.enterpriseai.backend.ai.exception.AiVectorStoreException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.SearchPoints;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class QdrantVectorStoreTest {

    private QdrantClient qdrantClient;
    private QdrantProperties properties;
    private QdrantVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        qdrantClient = Mockito.mock(QdrantClient.class);
        properties = new QdrantProperties("localhost", 6334, "test_collection", null);
        vectorStore = new QdrantVectorStore(qdrantClient, properties);
    }

    @Test
    void search_throwsAiVectorStoreException_onExecutionException() {
        when(qdrantClient.collectionExistsAsync(anyString()))
                .thenReturn(Futures.immediateFuture(true));

        when(qdrantClient.searchAsync(any(SearchPoints.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("API error")));

        assertThrows(AiVectorStoreException.class, () -> vectorStore.search(List.of(1.0, 2.0), 5, 5L, 1L));
    }

    @Test
    void upsert_throwsAiVectorStoreException_onExecutionException() {
        when(qdrantClient.collectionExistsAsync(anyString()))
                .thenReturn(Futures.immediateFuture(true));

        when(qdrantClient.upsertAsync(anyString(), any(List.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("API error")));

        assertThrows(AiVectorStoreException.class, () -> vectorStore.upsert(1L, List.of(1.0, 2.0), Map.of()));
    }

    @Test
    void delete_throwsAiVectorStoreException_onExecutionException() {
        when(qdrantClient.collectionExistsAsync(anyString()))
                .thenReturn(Futures.immediateFuture(true));
        
        // initialize collection state
        when(qdrantClient.searchAsync(any(SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of()));
        vectorStore.search(List.of(1.0), 1, 5L, 1L);

        when(qdrantClient.deleteAsync(anyString(), any(List.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("API error")));

        assertThrows(AiVectorStoreException.class, () -> vectorStore.delete(1L));
    }
}
