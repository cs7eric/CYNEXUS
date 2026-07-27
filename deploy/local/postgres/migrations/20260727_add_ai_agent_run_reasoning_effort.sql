\set ON_ERROR_STOP on

ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS reasoning_effort varchar(16);

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_run.reasoning_effort IS
    'Effective reasoning effort used for this Agent run';
