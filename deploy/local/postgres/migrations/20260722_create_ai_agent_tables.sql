CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_conversation (
    id bigint PRIMARY KEY,
    conversation_no varchar(64) NOT NULL,
    owner_key varchar(128) NOT NULL,
    title varchar(200) NOT NULL DEFAULT 'Cynexus Agent',
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    last_run_no varchar(64),
    last_message text,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_conversation_no UNIQUE (conversation_no)
);

CREATE INDEX IF NOT EXISTS ix_ai_agent_conversation_owner_updated
    ON cynexus_appl_ai.ai_agent_conversation (owner_key, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_run (
    id bigint PRIMARY KEY,
    run_no varchar(64) NOT NULL,
    conversation_no varchar(64) NOT NULL,
    owner_key varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    user_input text NOT NULL,
    answer text,
    model_code varchar(128),
    plan jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    turn_count integer NOT NULL DEFAULT 0,
    tool_call_count integer NOT NULL DEFAULT 0,
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_run_no UNIQUE (run_no)
);

CREATE INDEX IF NOT EXISTS ix_ai_agent_run_conversation_created
    ON cynexus_appl_ai.ai_agent_run (conversation_no, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_run_step (
    id bigint PRIMARY KEY,
    run_no varchar(64) NOT NULL,
    owner_key varchar(128) NOT NULL,
    step_no integer NOT NULL,
    kind varchar(32) NOT NULL,
    tool_code varchar(128),
    input_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    output_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(32) NOT NULL,
    summary text,
    source varchar(200),
    data_date varchar(32),
    error_message text,
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_run_step_owner_order UNIQUE (owner_key, run_no, step_no)
);

CREATE INDEX IF NOT EXISTS ix_ai_agent_run_step_run_order
    ON cynexus_appl_ai.ai_agent_run_step (owner_key, run_no, step_no)
    WHERE deleted_at IS NULL;
