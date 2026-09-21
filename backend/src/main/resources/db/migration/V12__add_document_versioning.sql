ALTER TABLE knowledge_documents 
ADD COLUMN parent_document_id BIGINT;

ALTER TABLE knowledge_documents 
ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE knowledge_documents
ADD CONSTRAINT fk_knowledge_documents_parent
FOREIGN KEY (parent_document_id) REFERENCES knowledge_documents(id);

CREATE INDEX idx_knowledge_documents_parent_id ON knowledge_documents(parent_document_id);
