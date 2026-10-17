\set ON_ERROR_STOP on

BEGIN;

-- Existing Cynexus installations are single-space by default.  Backfill only
-- missing identity so legacy documents can participate in the permission-aware
-- vector index; explicitly assigned tenant/owner values are preserved.
UPDATE cynexus_appl_knowledge_base.kb_document
   SET tenant_key = COALESCE(NULLIF(BTRIM(tenant_key), ''), 'default'),
       owner_key = COALESCE(NULLIF(BTRIM(owner_key), ''), 'default')
 WHERE deleted_at IS NULL
   AND (tenant_key IS NULL OR BTRIM(tenant_key) = '' OR owner_key IS NULL OR BTRIM(owner_key) = '');

COMMIT;
