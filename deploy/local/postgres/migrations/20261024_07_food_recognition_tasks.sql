\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_recognition_task (
    id BIGSERIAL PRIMARY KEY,
    owner_key VARCHAR(800) NOT NULL,
    tenant_key VARCHAR(128) NOT NULL,
    principal_key VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    command_json JSONB,
    task_status VARCHAR(24) NOT NULL DEFAULT 'queued',
    phase VARCHAR(24) NOT NULL DEFAULT 'submit',
    ai_task_id VARCHAR(128),
    prepared_json JSONB,
    failure_count INTEGER NOT NULL DEFAULT 0,
    retry_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    claimed_by VARCHAR(128),
    claimed_until TIMESTAMPTZ,
    food_id VARCHAR(128),
    food_name VARCHAR(200),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    cycle_started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_food_recognition_owner_request UNIQUE (owner_key,idempotency_key),
    CONSTRAINT ck_food_recognition_status CHECK (task_status IN ('queued','running','succeeded','failed')),
    CONSTRAINT ck_food_recognition_phase CHECK (phase IN ('submit','poll','save')),
    CONSTRAINT ck_food_recognition_attempts CHECK (failure_count>=0 AND retry_count BETWEEN 0 AND 20)
);

CREATE INDEX IF NOT EXISTS ix_food_recognition_queue
    ON cynexus_appl_life.food_recognition_task (available_at,id)
    WHERE task_status IN ('queued','running');
CREATE INDEX IF NOT EXISTS ix_food_recognition_owner
    ON cynexus_appl_life.food_recognition_task (owner_key,created_at DESC,id DESC);

COMMENT ON TABLE cynexus_appl_life.food_recognition_task IS
    'Core-owned nutrition recognition workflow. Original photos are erased atomically when the custom food is saved, or seven days after a terminal failure.';
