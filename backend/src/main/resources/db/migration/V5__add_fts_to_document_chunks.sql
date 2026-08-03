ALTER TABLE document_chunks 
ADD COLUMN search_vector tsvector 
GENERATED ALWAYS AS (to_tsvector('english', coalesce(content, ''))) STORED;

CREATE INDEX idx_document_chunks_search_vector ON document_chunks USING GIN(search_vector);
