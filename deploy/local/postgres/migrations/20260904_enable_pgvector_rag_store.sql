\set ON_ERROR_STOP on

BEGIN;

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

DO $$
BEGIN
    BEGIN
        EXECUTE 'CREATE TABLE IF NOT EXISTS cynexus_appl_ai.vector_store (id uuid DEFAULT gen_random_uuid() PRIMARY KEY, content text, metadata jsonb, embedding public.vector(1024))';
        EXECUTE 'CREATE INDEX IF NOT EXISTS cynexus_ai_vector_store_embedding_idx ON cynexus_appl_ai.vector_store USING hnsw (embedding vector_cosine_ops)';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'pgvector type/index is unavailable; creating disabled compatibility table: %', SQLERRM;
        EXECUTE 'CREATE TABLE IF NOT EXISTS cynexus_appl_ai.vector_store (id uuid DEFAULT gen_random_uuid() PRIMARY KEY, content text, metadata jsonb, embedding jsonb)';
    END;
END $$;

COMMIT;
