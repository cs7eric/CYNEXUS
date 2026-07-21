ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS idempotency_key varchar(128);

ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD COLUMN IF NOT EXISTS attempt_count integer NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_run_owner_idempotency
    ON cynexus_appl_ai.ai_agent_run (owner_key, idempotency_key)
    WHERE deleted_at IS NULL AND idempotency_key IS NOT NULL;
