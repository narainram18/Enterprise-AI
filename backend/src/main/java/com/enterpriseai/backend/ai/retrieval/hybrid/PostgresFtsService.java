package com.enterpriseai.backend.ai.retrieval.hybrid;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.enterpriseai.backend.ai.retrieval.model.RetrievedChunk;
import com.enterpriseai.backend.entity.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PostgresFtsService implements KeywordSearchService {

    private static final Logger log = LoggerFactory.getLogger(PostgresFtsService.class);
    private final JdbcTemplate jdbcTemplate;

    public PostgresFtsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RetrievedChunk> search(String query, Long workspaceId, Long userId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        
        // Convert to tsquery (replace spaces with &)
        String tsQuery = String.join(" & ", query.trim().split("\\s+"));
        
        String sql = """
            SELECT 
                dc.id as chunk_id,
                dc.document_id,
                dc.chunk_index,
                dc.content as chunk_text,
                kd.original_file_name,
                kd.document_type,
                kd.workspace_id as workspace_id,
                ts_rank(dc.search_vector, to_tsquery('english', ?)) as score
            FROM document_chunks dc
            JOIN knowledge_documents kd ON dc.document_id = kd.id
            WHERE kd.workspace_id = ? 
              AND kd.is_latest_version = true
              AND (kd.access_level = 'PUBLIC' OR kd.created_by_id = ?)
              AND dc.search_vector @@ to_tsquery('english', ?)
            ORDER BY score DESC
            LIMIT 50
        """;
        
        log.info("PostgresFtsService: executing keyword search for query '{}'", query);
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                return new RetrievedChunk(
                    rs.getLong("document_id"),
                    rs.getLong("chunk_id"),
                    rs.getInt("chunk_index"),
                    null, // pageNumber
                    rs.getDouble("score"), // BM25-like ts_rank score
                    rs.getString("original_file_name"),
                    DocumentType.valueOf(rs.getString("document_type")),
                    rs.getString("chunk_text"),
                    rs.getLong("workspace_id")
                );
            } catch (Exception e) {
                log.warn("Failed to map chunk from FTS", e);
                return null;
            }
        }, tsQuery, workspaceId, userId, tsQuery);
    }
}
