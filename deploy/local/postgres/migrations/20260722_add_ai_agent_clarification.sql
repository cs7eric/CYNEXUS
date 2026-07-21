ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS clarification_json jsonb NOT NULL DEFAULT 'null'::jsonb;
