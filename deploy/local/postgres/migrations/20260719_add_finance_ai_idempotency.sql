\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE cynexus_appl_core.finance_ai_captures
    ADD COLUMN IF NOT EXISTS idempotency_key varchar(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_ai_captures_owner_idempotency
    ON cynexus_appl_core.finance_ai_captures (owner_key, idempotency_key)
    WHERE deleted_at IS NULL AND idempotency_key IS NOT NULL;

COMMIT;
