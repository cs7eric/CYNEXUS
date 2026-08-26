ALTER TABLE cynexus_appl_core.finance_ai_captures
    DROP CONSTRAINT IF EXISTS ck_finance_ai_capture_status;

ALTER TABLE cynexus_appl_core.finance_ai_captures
    ADD CONSTRAINT ck_finance_ai_capture_status
    CHECK (status IN ('uploaded', 'queued', 'processing', 'review', 'confirmed', 'failed', 'archived'));
