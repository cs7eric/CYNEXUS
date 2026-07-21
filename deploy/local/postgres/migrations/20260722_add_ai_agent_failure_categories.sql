ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS failure_category varchar(64);

ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD COLUMN IF NOT EXISTS failure_category varchar(64);

CREATE INDEX IF NOT EXISTS idx_ai_agent_run_stale
    ON cynexus_appl_ai.ai_agent_run (status, started_at)
    WHERE status = 'RUNNING' AND finished_at IS NULL AND deleted_at IS NULL;
