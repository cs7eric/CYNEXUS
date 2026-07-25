ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS provider_code varchar(128);

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_run.provider_code IS
    'Provider code selected for this Agent run, used to preserve the route for retry and regeneration.';
