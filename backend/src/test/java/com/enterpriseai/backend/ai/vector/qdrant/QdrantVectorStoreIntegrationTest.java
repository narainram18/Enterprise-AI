package com.enterpriseai.backend.ai.vector.qdrant;

import com.enterpriseai.backend.ai.vector.dto.VectorSearchResult;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Requires Docker for Testcontainers")
@Testcontainers
class QdrantVectorStoreIntegrationTest {

    @Container
    private static final QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.11.1");

    private static QdrantClient qdrantClient;
    private QdrantVectorStore vectorStore;
    private QdrantProperties properties;

    @BeforeAll
    static void setUpAll() {
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                qdrant.getHost(),
                qdrant.getMappedPort(6334),
                false
        );
        qdrantClient = new QdrantClient(grpcClientBuilder.build());
    }

    @AfterAll
    static void tearDownAll() {
        if (qdrantClient != null) {
            qdrantClient.close();
        }
    }

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        String testCollection = "test_collection_" + UUID.randomUUID().toString().replace("-", "");
        properties = new QdrantProperties(qdrant.getHost(), qdrant.getMappedPort(6334), testCollection, null);
        vectorStore = new QdrantVectorStore(qdrantClient, properties);
    }

    @Test
    void testEndToEnd() throws ExecutionException, InterruptedException {
        Long docId1 = 101L;
        Long chunk1 = 1001L;
        Long chunk2 = 1002L;

        // 1. Insert vectors
        vectorStore.upsert(
                chunk1,
                List.of(1.0, 0.0, 0.0),
                Map.of("documentId", docId1, "type", "test")
        );

        vectorStore.upsertBatch(
                List.of(chunk2),
                List.of(List.of(0.0, 1.0, 0.0)),
                List.of(Map.of("documentId", docId1, "type", "test2"))
        );

        // 2. Search exact match chunk 1
        List<VectorSearchResult> results1 = vectorStore.search(List.of(1.0, 0.0, 0.0), 5);
        assertEquals(2, results1.size());
        assertEquals(chunk1, results1.get(0).chunkId());
        assertTrue(results1.get(0).score() > 0.9);
        assertEquals(docId1, results1.get(0).metadata().get("documentId"));

        // 3. Search exact match chunk 2
        List<VectorSearchResult> results2 = vectorStore.search(List.of(0.0, 1.0, 0.0), 5);
        assertEquals(2, results2.size());
        assertEquals(chunk2, results2.get(0).chunkId());

        // 4. Delete chunk 1
        vectorStore.delete(chunk1);
        List<VectorSearchResult> resultsAfterDelete = vectorStore.search(List.of(1.0, 0.0, 0.0), 5);
        assertEquals(1, resultsAfterDelete.size());
        assertEquals(chunk2, resultsAfterDelete.get(0).chunkId());

        // 5. Delete by document
        vectorStore.deleteByDocument(docId1);
        
        // Wait a tiny bit for qdrant filter delete propagation if needed, though usually immediate
        Thread.sleep(100);

        List<VectorSearchResult> resultsAfterDeleteDoc = vectorStore.search(List.of(0.0, 1.0, 0.0), 5);
        assertEquals(0, resultsAfterDeleteDoc.size());

        // Clean up collection manually
        qdrantClient.deleteCollectionAsync(properties.collection()).get();
    }
}
