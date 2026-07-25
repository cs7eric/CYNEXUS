ALTER TABLE cynexus_appl_ai.ai_agent_capability
    ADD COLUMN IF NOT EXISTS guidance_json jsonb NOT NULL DEFAULT '{}'::jsonb;
