\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE cynexus_appl_knowledge_base.kb_document
    ADD COLUMN IF NOT EXISTS tenant_key varchar(128),
    ADD COLUMN IF NOT EXISTS owner_key varchar(128);

CREATE INDEX IF NOT EXISTS idx_kb_document_identity
    ON cynexus_appl_knowledge_base.kb_document (tenant_key, owner_key, status)
    WHERE deleted_at IS NULL;

COMMIT;
