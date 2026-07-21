ALTER TABLE cynexus_appl_core.finance_ai_captures
    ADD COLUMN IF NOT EXISTS processing_mode varchar(16) NOT NULL DEFAULT 'auto';

CREATE INDEX IF NOT EXISTS ix_finance_ai_captures_owner_status_created
    ON cynexus_appl_core.finance_ai_captures (owner_key, status, created_at DESC)
    WHERE deleted_at IS NULL;
