CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_message (
    id bigint PRIMARY KEY,
    conversation_no varchar(64) NOT NULL,
    run_no varchar(64),
    owner_key varchar(128) NOT NULL,
    sequence_no integer NOT NULL,
    role varchar(32) NOT NULL,
    content text,
    tool_call_id varchar(128),
    tool_name varchar(128),
    tool_calls jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_message_sequence UNIQUE (conversation_no, sequence_no),
    CONSTRAINT uk_ai_agent_message_owner_sequence UNIQUE (owner_key, conversation_no, sequence_no)
);

CREATE INDEX IF NOT EXISTS ix_ai_agent_message_conversation_order
    ON cynexus_appl_ai.ai_agent_message (owner_key, conversation_no, sequence_no)
    WHERE deleted_at IS NULL;
