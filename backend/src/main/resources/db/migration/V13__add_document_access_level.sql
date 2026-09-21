ALTER TABLE knowledge_documents 
ADD COLUMN access_level VARCHAR(32) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE knowledge_documents 
ADD COLUMN is_latest_version BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX idx_knowledge_documents_access_level ON knowledge_documents(access_level);
CREATE INDEX idx_knowledge_documents_is_latest_version ON knowledge_documents(is_latest_version);
