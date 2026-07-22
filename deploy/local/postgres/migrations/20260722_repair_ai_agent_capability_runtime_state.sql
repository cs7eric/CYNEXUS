-- Runtime discovery must not permanently disable a capability through the governance flag.
-- Restore rows affected by the old synchronization behavior, while preserving an explicit
-- Provider-page disablement recorded after the latest live handler observation.
UPDATE cynexus_appl_ai.ai_agent_capability
SET is_enabled = true,
    availability = 'AVAILABLE',
    stale_at = now() + interval '2 hours',
    updated_at = now()
WHERE deleted_at IS NULL
  AND governance_availability = 'AVAILABLE'
  AND is_enabled = false
  AND availability IN ('AVAILABLE', 'STALE')
  AND (stale_at IS NULL OR stale_at <= now())
  AND NOT EXISTS (
      SELECT 1
      FROM cynexus_appl_ai.ai_agent_capability_audit governed
      WHERE governed.capability_code = ai_agent_capability.capability_code
        AND governed.action = 'GOVERNANCE_UPDATED'
        AND governed.occurred_at > COALESCE((
            SELECT max(observed.occurred_at)
            FROM cynexus_appl_ai.ai_agent_capability_audit observed
            WHERE observed.capability_code = ai_agent_capability.capability_code
              AND observed.version = ai_agent_capability.version
              AND observed.action = 'REGISTERED'
        ), '-infinity'::timestamptz)
  );

-- Keep Agent persistence aligned with the 128-character owner-key convention.
ALTER TABLE cynexus_appl_ai.ai_agent_task ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_feedback ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_usage_daily ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_write_command ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_write_audit ALTER COLUMN owner_key TYPE varchar(128);

-- Child transcript rows carry the same owner boundary as their parent.  Existing
-- rows are backfilled before the column becomes mandatory.
ALTER TABLE cynexus_appl_ai.ai_agent_message
    ADD COLUMN IF NOT EXISTS owner_key varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD COLUMN IF NOT EXISTS owner_key varchar(128);
UPDATE cynexus_appl_ai.ai_agent_message message
SET owner_key = conversation.owner_key
FROM cynexus_appl_ai.ai_agent_conversation conversation
WHERE message.owner_key IS NULL
  AND message.conversation_no = conversation.conversation_no;
UPDATE cynexus_appl_ai.ai_agent_run_step step
SET owner_key = run.owner_key
FROM cynexus_appl_ai.ai_agent_run run
WHERE step.owner_key IS NULL
  AND step.run_no = run.run_no;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cynexus_appl_ai.ai_agent_message WHERE owner_key IS NULL)
       OR EXISTS (SELECT 1 FROM cynexus_appl_ai.ai_agent_run_step WHERE owner_key IS NULL) THEN
        RAISE EXCEPTION 'Cannot establish owner boundary for Agent child rows';
    END IF;
END $$;
ALTER TABLE cynexus_appl_ai.ai_agent_message ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_message ALTER COLUMN owner_key SET NOT NULL;
ALTER TABLE cynexus_appl_ai.ai_agent_run_step ALTER COLUMN owner_key TYPE varchar(128);
ALTER TABLE cynexus_appl_ai.ai_agent_run_step ALTER COLUMN owner_key SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_conversation_owner_no') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_conversation
            ADD CONSTRAINT uk_ai_agent_conversation_owner_no UNIQUE (owner_key, conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_run_owner_no') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run
            ADD CONSTRAINT uk_ai_agent_run_owner_no UNIQUE (owner_key, run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_write_command_owner_no') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_command
            ADD CONSTRAINT uk_ai_agent_write_command_owner_no UNIQUE (owner_key, command_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_message_owner_sequence') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            ADD CONSTRAINT uk_ai_agent_message_owner_sequence UNIQUE (owner_key, conversation_no, sequence_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_run_step_owner_order') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step
            ADD CONSTRAINT uk_ai_agent_run_step_owner_order UNIQUE (owner_key, run_no, step_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_run_event_owner_sequence') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_event
            ADD CONSTRAINT uk_ai_agent_run_event_owner_sequence UNIQUE (owner_key, run_no, sequence_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_agent_task_owner_no') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task
            ADD CONSTRAINT uk_ai_agent_task_owner_no UNIQUE (owner_key, task_no);
    END IF;
END $$;

-- Replace the earlier unqualified child FKs. Dropping and recreating by the
-- stable names also makes this repair safe on databases upgraded from the first
-- Agent migration bundle.
ALTER TABLE cynexus_appl_ai.ai_agent_run
    DROP CONSTRAINT IF EXISTS fk_ai_agent_run_conversation;
ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD CONSTRAINT fk_ai_agent_run_conversation FOREIGN KEY (owner_key, conversation_no)
    REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
ALTER TABLE cynexus_appl_ai.ai_agent_message
    DROP CONSTRAINT IF EXISTS fk_ai_agent_message_conversation;
ALTER TABLE cynexus_appl_ai.ai_agent_message
    ADD CONSTRAINT fk_ai_agent_message_conversation FOREIGN KEY (owner_key, conversation_no)
    REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
ALTER TABLE cynexus_appl_ai.ai_agent_message
    DROP CONSTRAINT IF EXISTS fk_ai_agent_message_run;
ALTER TABLE cynexus_appl_ai.ai_agent_message
    ADD CONSTRAINT fk_ai_agent_message_run FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    DROP CONSTRAINT IF EXISTS fk_ai_agent_run_step_run;
ALTER TABLE cynexus_appl_ai.ai_agent_run_step
    ADD CONSTRAINT fk_ai_agent_run_step_run FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_run_event
    DROP CONSTRAINT IF EXISTS fk_ai_agent_run_event_run_owner;
ALTER TABLE cynexus_appl_ai.ai_agent_run_event
    ADD CONSTRAINT fk_ai_agent_run_event_run_owner FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_task
    DROP CONSTRAINT IF EXISTS fk_ai_agent_task_conversation;
ALTER TABLE cynexus_appl_ai.ai_agent_task
    ADD CONSTRAINT fk_ai_agent_task_conversation FOREIGN KEY (owner_key, conversation_no)
    REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
ALTER TABLE cynexus_appl_ai.ai_agent_task
    DROP CONSTRAINT IF EXISTS fk_ai_agent_task_run;
ALTER TABLE cynexus_appl_ai.ai_agent_task
    ADD CONSTRAINT fk_ai_agent_task_run FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_feedback
    DROP CONSTRAINT IF EXISTS fk_ai_agent_feedback_run_owner;
ALTER TABLE cynexus_appl_ai.ai_agent_feedback
    ADD CONSTRAINT fk_ai_agent_feedback_run_owner FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_feedback
    DROP CONSTRAINT IF EXISTS fk_ai_agent_feedback_conversation;
ALTER TABLE cynexus_appl_ai.ai_agent_feedback
    ADD CONSTRAINT fk_ai_agent_feedback_conversation FOREIGN KEY (owner_key, conversation_no)
    REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
ALTER TABLE cynexus_appl_ai.ai_agent_write_command
    DROP CONSTRAINT IF EXISTS fk_ai_agent_write_command_run;
ALTER TABLE cynexus_appl_ai.ai_agent_write_command
    ADD CONSTRAINT fk_ai_agent_write_command_run FOREIGN KEY (owner_key, run_no)
    REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
ALTER TABLE cynexus_appl_ai.ai_agent_write_command
    DROP CONSTRAINT IF EXISTS fk_ai_agent_write_command_conversation;
ALTER TABLE cynexus_appl_ai.ai_agent_write_command
    ADD CONSTRAINT fk_ai_agent_write_command_conversation FOREIGN KEY (owner_key, conversation_no)
    REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
ALTER TABLE cynexus_appl_ai.ai_agent_write_audit
    DROP CONSTRAINT IF EXISTS fk_ai_agent_write_audit_command_owner;
ALTER TABLE cynexus_appl_ai.ai_agent_write_audit
    ADD CONSTRAINT fk_ai_agent_write_audit_command_owner FOREIGN KEY (owner_key, command_no)
    REFERENCES cynexus_appl_ai.ai_agent_write_command (owner_key, command_no);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_nonnegative_usage') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run ADD CONSTRAINT ck_ai_agent_run_nonnegative_usage CHECK (
            turn_count >= 0 AND tool_call_count >= 0 AND input_tokens >= 0
            AND output_tokens >= 0 AND total_tokens >= 0 AND estimated_cost >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_step_nonnegative') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step ADD CONSTRAINT ck_ai_agent_run_step_nonnegative
            CHECK (step_no >= 0 AND attempt_count > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_event_sequence_positive') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_event ADD CONSTRAINT ck_ai_agent_run_event_sequence_positive
            CHECK (sequence_no > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_usage_nonnegative') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_usage_daily ADD CONSTRAINT ck_ai_agent_usage_nonnegative CHECK (
            request_count >= 0 AND reserved_tokens >= 0 AND input_tokens >= 0
            AND output_tokens >= 0 AND total_tokens >= 0 AND estimated_cost >= 0
            AND estimated_usage_count >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_task_lease_shape') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task ADD CONSTRAINT ck_ai_agent_task_lease_shape CHECK (
            (status = 'RUNNING' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL)
            OR (status <> 'RUNNING' AND lease_owner IS NULL AND lease_until IS NULL));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_message_sequence_positive') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message ADD CONSTRAINT ck_ai_agent_message_sequence_positive
            CHECK (sequence_no > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_owner_keys_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_conversation ADD CONSTRAINT ck_ai_agent_owner_keys_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_message_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message ADD CONSTRAINT ck_ai_agent_message_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_step_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step ADD CONSTRAINT ck_ai_agent_run_step_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_write_expiry_after_creation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_command ADD CONSTRAINT ck_ai_agent_write_expiry_after_creation
            CHECK (expires_at > created_at);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run ADD CONSTRAINT ck_ai_agent_run_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_run_event_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_event ADD CONSTRAINT ck_ai_agent_run_event_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_task_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task ADD CONSTRAINT ck_ai_agent_task_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_feedback_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_feedback ADD CONSTRAINT ck_ai_agent_feedback_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_usage_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_usage_daily ADD CONSTRAINT ck_ai_agent_usage_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_write_command_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_command ADD CONSTRAINT ck_ai_agent_write_command_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_write_audit_owner_nonblank') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_audit ADD CONSTRAINT ck_ai_agent_write_audit_owner_nonblank
            CHECK (length(btrim(owner_key)) BETWEEN 1 AND 128);
    END IF;
END $$;

ALTER TABLE cynexus_appl_ai.ai_agent_task
    DROP CONSTRAINT IF EXISTS ck_ai_agent_task_attempts_valid;
ALTER TABLE cynexus_appl_ai.ai_agent_task
    ADD CONSTRAINT ck_ai_agent_task_attempts_valid
    CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts);

DROP INDEX IF EXISTS cynexus_appl_ai.ix_ai_agent_task_running_lease;
CREATE INDEX ix_ai_agent_task_running_lease
    ON cynexus_appl_ai.ai_agent_task (owner_key, lease_until, updated_at)
    WHERE status = 'RUNNING' AND deleted_at IS NULL;
DROP INDEX IF EXISTS cynexus_appl_ai.ix_ai_agent_run_conversation_created;
CREATE INDEX ix_ai_agent_run_conversation_created
    ON cynexus_appl_ai.ai_agent_run (owner_key, conversation_no, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_run_owner_status_created
    ON cynexus_appl_ai.ai_agent_run (owner_key, status, created_at DESC)
    WHERE deleted_at IS NULL;
DROP INDEX IF EXISTS cynexus_appl_ai.ix_ai_agent_run_step_status_order;
CREATE INDEX ix_ai_agent_run_step_status_order
    ON cynexus_appl_ai.ai_agent_run_step (owner_key, run_no, status, step_no)
    WHERE deleted_at IS NULL;
DROP INDEX IF EXISTS cynexus_appl_ai.ix_ai_agent_message_conversation_order;
CREATE INDEX ix_ai_agent_message_conversation_order
    ON cynexus_appl_ai.ai_agent_message (owner_key, conversation_no, sequence_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_feedback_owner_created
    ON cynexus_appl_ai.ai_agent_feedback (owner_key, created_at DESC)
    WHERE deleted_at IS NULL;
DROP INDEX IF EXISTS cynexus_appl_ai.ix_ai_agent_usage_tenant_date;
CREATE INDEX ix_ai_agent_usage_tenant_date
    ON cynexus_appl_ai.ai_agent_usage_daily (owner_key, tenant_key, usage_date DESC);
CREATE INDEX IF NOT EXISTS ix_ai_agent_write_command_owner_status_expiry
    ON cynexus_appl_ai.ai_agent_write_command (owner_key, status, expires_at)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_ai.ai_agent_run_event IS
    'Owner-scoped append-only public workflow events for SSE replay; hidden model reasoning is excluded.';
COMMENT ON TABLE cynexus_appl_ai.ai_agent_write_audit IS
    'Owner-scoped audit trail for preview, confirmation, rejection and execution of Agent write commands.';
