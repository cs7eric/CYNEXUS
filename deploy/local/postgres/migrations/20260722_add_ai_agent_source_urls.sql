ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD COLUMN IF NOT EXISTS source_url varchar(500);
