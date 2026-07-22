CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_task_notification (
    id bigint PRIMARY KEY,
    event_key varchar(256) NOT NULL UNIQUE,
    owner_key varchar(128) NOT NULL,
    tenant_key varchar(128) NOT NULL,
    principal_key varchar(128) NOT NULL,
    task_no varchar(64) NOT NULL,
    event_type varchar(32) NOT NULL,
    payload_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz,
    last_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

COMMENT ON TABLE cynexus_appl_ai.ai_agent_task_notification IS
    'Durable Agent task lifecycle outbox. PENDING means persisted, not externally delivered.';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_ai_agent_task_notification_status'
          AND conrelid = 'cynexus_appl_ai.ai_agent_task_notification'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task_notification
            ADD CONSTRAINT ck_ai_agent_task_notification_status
            CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_ai_agent_task_notification_event_type'
          AND conrelid = 'cynexus_appl_ai.ai_agent_task_notification'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task_notification
            ADD CONSTRAINT ck_ai_agent_task_notification_event_type
            CHECK (event_type IN ('COMPLETED', 'RETRY_SCHEDULED', 'DEAD_LETTER'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_ai_agent_task_notification_attempt_count'
          AND conrelid = 'cynexus_appl_ai.ai_agent_task_notification'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task_notification
            ADD CONSTRAINT ck_ai_agent_task_notification_attempt_count
            CHECK (attempt_count >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_agent_task_notification_pending
    ON cynexus_appl_ai.ai_agent_task_notification (status, next_attempt_at, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_agent_task_notification_owner
    ON cynexus_appl_ai.ai_agent_task_notification (owner_key, created_at DESC);
