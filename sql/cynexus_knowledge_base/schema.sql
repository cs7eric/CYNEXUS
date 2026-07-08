\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_knowledge_base;

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_document (
    id bigint PRIMARY KEY,
    title varchar(256) NOT NULL,
    description text,
    content text NOT NULL DEFAULT '',
    content_format varchar(32) NOT NULL DEFAULT 'markdown',
    status varchar(32) NOT NULL DEFAULT 'draft',
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_kb_document_status CHECK (status IN ('draft', 'published', 'archived')),
    CONSTRAINT ck_kb_document_format CHECK (content_format IN ('markdown'))
);

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_tag (
    id bigint PRIMARY KEY,
    tag_code varchar(128) NOT NULL,
    tag_name varchar(128) NOT NULL,
    description text,
    color varchar(32),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_category (
    id bigint PRIMARY KEY,
    category_code varchar(128) NOT NULL,
    category_name varchar(128) NOT NULL,
    description text,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_document_tag_mapping (
    id bigint PRIMARY KEY,
    document_id bigint NOT NULL,
    tag_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_document_category_mapping (
    id bigint PRIMARY KEY,
    document_id bigint NOT NULL,
    category_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_tag_code_alive
    ON cynexus_appl_knowledge_base.kb_tag (tag_code)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_category_code_alive
    ON cynexus_appl_knowledge_base.kb_category (category_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_status_alive
    ON cynexus_appl_knowledge_base.kb_document (status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_title_alive
    ON cynexus_appl_knowledge_base.kb_document (title)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_document_tag_alive
    ON cynexus_appl_knowledge_base.kb_document_tag_mapping (document_id, tag_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_tag_tag_alive
    ON cynexus_appl_knowledge_base.kb_document_tag_mapping (tag_id, document_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_document_category_alive
    ON cynexus_appl_knowledge_base.kb_document_category_mapping (document_id, category_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_category_category_alive
    ON cynexus_appl_knowledge_base.kb_document_category_mapping (category_id, document_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_document_chunk (
    id bigint PRIMARY KEY,
    document_id bigint NOT NULL,
    chunk_index integer NOT NULL,
    heading varchar(256),
    content text NOT NULL,
    content_hash varchar(64) NOT NULL,
    token_estimate integer NOT NULL DEFAULT 0,
    embedding_provider varchar(64),
    embedding_model varchar(128),
    embedding_status varchar(32) NOT NULL DEFAULT 'pending',
    embedding_vector_json jsonb,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_kb_document_chunk_embedding_status CHECK (embedding_status IN ('pending', 'embedded', 'failed', 'skipped'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_document_chunk_alive
    ON cynexus_appl_knowledge_base.kb_document_chunk (document_id, chunk_index)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_chunk_document_alive
    ON cynexus_appl_knowledge_base.kb_document_chunk (document_id, chunk_index)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_kb_document_chunk_embedding_alive
    ON cynexus_appl_knowledge_base.kb_document_chunk (embedding_status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_knowledge_base.kb_document_index_task (
    id bigint PRIMARY KEY,
    document_id bigint NOT NULL,
    target varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'pending',
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_kb_document_index_task_target CHECK (target IN ('pgvector', 'elasticsearch')),
    CONSTRAINT ck_kb_document_index_task_status CHECK (status IN ('pending', 'processing', 'succeeded', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_kb_document_index_task_status_alive
    ON cynexus_appl_knowledge_base.kb_document_index_task (target, status, updated_at DESC)
    WHERE deleted_at IS NULL;

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN undefined_file THEN
        RAISE NOTICE 'pgvector extension is not installed; kb_document_chunk.embedding_vector column is skipped.';
    WHEN feature_not_supported THEN
        RAISE NOTICE 'pgvector extension is not available in this PostgreSQL image; kb_document_chunk.embedding_vector column is skipped.';
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
        ALTER TABLE cynexus_appl_knowledge_base.kb_document_chunk
            ADD COLUMN IF NOT EXISTS embedding_vector vector(1024);
    END IF;
END $$;
