CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_feedback (
    id bigint PRIMARY KEY,
    owner_key varchar(255) NOT NULL,
    tenant_key varchar(128) NOT NULL,
    principal_key varchar(128) NOT NULL,
    run_no varchar(64) NOT NULL,
    conversation_no varchar(64),
    rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
    feedback_reason varchar(120),
    correction_text varchar(2000),
    evaluation_label varchar(32) NOT NULL DEFAULT 'UNVERIFIED',
    reviewer_key varchar(255),
    reviewer_note varchar(2000),
    reviewed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_feedback_owner_run UNIQUE (owner_key, run_no)
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_feedback_tenant_created
    ON cynexus_appl_ai.ai_agent_feedback (tenant_key, created_at DESC);
