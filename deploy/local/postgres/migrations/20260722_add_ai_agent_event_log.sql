CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_run_event (
    id bigint PRIMARY KEY,
    run_no varchar(64) NOT NULL,
    owner_key varchar(128) NOT NULL,
    sequence_no integer NOT NULL,
    event_type varchar(64) NOT NULL,
    status varchar(32),
    step_no integer,
    tool_code varchar(128),
    summary text,
    answer text,
    delta text,
    error_message text,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_run_event_sequence UNIQUE (run_no, sequence_no)
);

CREATE INDEX IF NOT EXISTS ix_ai_agent_run_event_owner_run_sequence
    ON cynexus_appl_ai.ai_agent_run_event (owner_key, run_no, sequence_no)
    WHERE deleted_at IS NULL;
