CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_task (
    id bigint PRIMARY KEY,
    task_no varchar(64) NOT NULL UNIQUE,
    owner_key varchar(128) NOT NULL,
    tenant_key varchar(128) NOT NULL,
    principal_key varchar(128) NOT NULL,
    conversation_no varchar(64),
    user_input varchar(12000) NOT NULL,
    preferred_model_code varchar(128),
    schedule_at timestamptz NOT NULL,
    status varchar(32) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 3,
    lease_owner varchar(128),
    lease_until timestamptz,
    next_attempt_at timestamptz,
    run_no varchar(64),
    last_error text,
    paused_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

ALTER TABLE cynexus_appl_ai.ai_agent_task
    ALTER COLUMN last_error TYPE text;

CREATE INDEX IF NOT EXISTS idx_ai_agent_task_due
    ON cynexus_appl_ai.ai_agent_task (status, schedule_at, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_ai_agent_task_owner_created
    ON cynexus_appl_ai.ai_agent_task (owner_key, created_at DESC);
