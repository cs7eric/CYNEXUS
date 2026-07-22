ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS cancellation_requested_at timestamptz;

ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS cancellation_requested_by varchar(128);

CREATE INDEX IF NOT EXISTS ix_ai_agent_run_cancellation_requested
    ON cynexus_appl_ai.ai_agent_run (owner_key, cancellation_requested_at)
    WHERE cancellation_requested_at IS NOT NULL AND finished_at IS NULL AND deleted_at IS NULL;
