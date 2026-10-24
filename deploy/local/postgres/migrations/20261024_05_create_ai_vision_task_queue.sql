\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_ai;

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_vision_task (
    id BIGSERIAL PRIMARY KEY,
    scenario_code VARCHAR(128) NOT NULL,
    user_input TEXT,
    preferred_model_code VARCHAR(255),
    images_json JSONB NOT NULL,
    tenant_key VARCHAR(128),
    principal_key VARCHAR(255),
    idempotency_key VARCHAR(255),
    task_status VARCHAR(32) NOT NULL DEFAULT 'queued',
    progress SMALLINT NOT NULL DEFAULT 0,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 2,
    claimed_by VARCHAR(128),
    execution_no VARCHAR(128),
    provider_code VARCHAR(128),
    model_code VARCHAR(255),
    result_content TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_vision_task_status CHECK (task_status IN ('queued', 'running', 'succeeded', 'failed')),
    CONSTRAINT ck_ai_vision_task_progress CHECK (progress BETWEEN 0 AND 100),
    CONSTRAINT ck_ai_vision_task_attempts CHECK (attempt_count >= 0 AND max_attempts > 0),
    CONSTRAINT ck_ai_vision_task_images CHECK (jsonb_typeof(images_json) = 'array')
);

CREATE INDEX IF NOT EXISTS ix_ai_vision_task_queue
    ON cynexus_appl_ai.ai_vision_task (task_status, created_at, id);
CREATE INDEX IF NOT EXISTS ix_ai_vision_task_owner
    ON cynexus_appl_ai.ai_vision_task (tenant_key, principal_key, created_at DESC, id DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_vision_task_owner_idempotency
    ON cynexus_appl_ai.ai_vision_task (tenant_key, principal_key, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON TABLE cynexus_appl_ai.ai_vision_task IS
    'Asynchronous vision execution queue. Image data is retained only until the task result is available.';
