ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD COLUMN IF NOT EXISTS citation_json jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_run_step.citation_json IS
    'Redacted, tool-backed citation metadata; model-only claims are never stored here.';
