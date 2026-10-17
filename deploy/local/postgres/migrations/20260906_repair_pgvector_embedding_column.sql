\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    row_count bigint;
    embedding_type text;
BEGIN
    IF to_regclass('cynexus_appl_ai.vector_store') IS NULL
       OR to_regtype('public.vector') IS NULL THEN
        RAISE NOTICE 'Skipping pgvector repair because vector_store or public.vector is unavailable';
        RETURN;
    END IF;

    SELECT udt_name INTO embedding_type
      FROM information_schema.columns
     WHERE table_schema = 'cynexus_appl_ai'
       AND table_name = 'vector_store'
       AND column_name = 'embedding';

    IF embedding_type = 'jsonb' THEN
        SELECT count(*) INTO row_count FROM cynexus_appl_ai.vector_store;
        IF row_count > 0 THEN
            RAISE EXCEPTION 'Cannot convert populated jsonb vector_store without a lossless embedding migration';
        END IF;
        BEGIN
            ALTER TABLE cynexus_appl_ai.vector_store
                ALTER COLUMN embedding TYPE public.vector(1024) USING NULL;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'pgvector type is present but unusable in this image; leaving compatibility jsonb column: %', SQLERRM;
            RETURN;
        END;
    END IF;

    BEGIN
        CREATE INDEX IF NOT EXISTS cynexus_ai_vector_store_embedding_idx
            ON cynexus_appl_ai.vector_store USING hnsw (embedding vector_cosine_ops);
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'Skipping pgvector index because the installed image does not provide a usable vector operator: %', SQLERRM;
    END;
END $$;

COMMIT;
