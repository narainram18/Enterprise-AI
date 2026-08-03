package com.enterpriseai.backend.workspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.ai.vector.VectorStore;
import com.enterpriseai.backend.entity.KnowledgeDocument;
import com.enterpriseai.backend.repository.KnowledgeDocumentRepository;
import com.enterpriseai.backend.repository.DocumentChunkRepository;

import com.enterpriseai.backend.ai.vector.qdrant.QdrantProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt.Value;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.vector.provider", havingValue = "qdrant")
public class QdrantBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantBackfillRunner.class);
    
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;

    public QdrantBackfillRunner(KnowledgeDocumentRepository knowledgeDocumentRepository,
                                QdrantClient qdrantClient,
                                QdrantProperties qdrantProperties) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.qdrantClient = qdrantClient;
        this.qdrantProperties = qdrantProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Qdrant workspaceId backfill...");
        
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findAll();
        for (KnowledgeDocument doc : documents) {
            if (doc.getWorkspace() == null) {
                log.warn("Document ID {} has no workspace. Skipping Qdrant backfill.", doc.getId());
                continue;
            }
            
            Long workspaceId = doc.getWorkspace().getId();
            Long createdById = doc.getCreatedBy() != null ? doc.getCreatedBy().getId() : null;
            
            try {
                Filter filter = Filter.newBuilder()
                        .addMust(ConditionFactory.match("documentId", doc.getId()))
                        .build();

                Map<String, Value> payload = new HashMap<>();
                payload.put("workspaceId", ValueFactory.value(workspaceId));
                if (createdById != null) {
                    payload.put("createdById", ValueFactory.value(createdById));
                }

                qdrantClient.setPayloadAsync(
                        qdrantProperties.collection(),
                        payload,
                        filter,
                        true,
                        null,
                        null).get();
                log.info("Successfully backfilled Qdrant payload for document {}", doc.getId());
            } catch (Exception e) {
                log.error("Failed to backfill document {}", doc.getId(), e);
            }
        }
        log.info("Finished Qdrant workspaceId backfill.");
    }
}
