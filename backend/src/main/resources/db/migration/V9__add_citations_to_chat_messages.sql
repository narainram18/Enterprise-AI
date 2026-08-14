ALTER TABLE chat_messages
ADD COLUMN citations JSONB,
ADD COLUMN retrieval_statistics JSONB;
