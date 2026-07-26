ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS cached_input_tokens integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cache_write_tokens integer NOT NULL DEFAULT 0;
