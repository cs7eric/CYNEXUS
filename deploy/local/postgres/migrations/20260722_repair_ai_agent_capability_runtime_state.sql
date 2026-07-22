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
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_run_conversation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run
            ADD CONSTRAINT fk_ai_agent_run_conversation FOREIGN KEY (owner_key, conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_message_conversation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            ADD CONSTRAINT fk_ai_agent_message_conversation FOREIGN KEY (conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_message_run') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            ADD CONSTRAINT fk_ai_agent_message_run FOREIGN KEY (run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_run_step_run') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step
            ADD CONSTRAINT fk_ai_agent_run_step_run FOREIGN KEY (run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_run_event_run_owner') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_event
            ADD CONSTRAINT fk_ai_agent_run_event_run_owner FOREIGN KEY (owner_key, run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_task_conversation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task
            ADD CONSTRAINT fk_ai_agent_task_conversation FOREIGN KEY (owner_key, conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_task_run') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task
            ADD CONSTRAINT fk_ai_agent_task_run FOREIGN KEY (owner_key, run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_feedback_run_owner') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_feedback
            ADD CONSTRAINT fk_ai_agent_feedback_run_owner FOREIGN KEY (owner_key, run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_feedback_conversation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_feedback
            ADD CONSTRAINT fk_ai_agent_feedback_conversation FOREIGN KEY (owner_key, conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_write_command_run') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_command
            ADD CONSTRAINT fk_ai_agent_write_command_run FOREIGN KEY (owner_key, run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_write_command_conversation') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_command
            ADD CONSTRAINT fk_ai_agent_write_command_conversation FOREIGN KEY (owner_key, conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_agent_write_audit_command_owner') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_write_audit
            ADD CONSTRAINT fk_ai_agent_write_audit_command_owner FOREIGN KEY (owner_key, command_no)
            REFERENCES cynexus_appl_ai.ai_agent_write_command (owner_key, command_no);
    END IF;
END $$;

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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_task_attempts_valid') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_task ADD CONSTRAINT ck_ai_agent_task_attempts_valid
            CHECK (attempt_count >= 0 AND max_attempts > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_agent_usage_nonnegative') THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_usage_daily ADD CONSTRAINT ck_ai_agent_usage_nonnegative CHECK (
            request_count >= 0 AND reserved_tokens >= 0 AND input_tokens >= 0
            AND output_tokens >= 0 AND total_tokens >= 0 AND estimated_cost >= 0
            AND estimated_usage_count >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_ai_agent_task_running_lease
    ON cynexus_appl_ai.ai_agent_task (lease_until, updated_at)
    WHERE status = 'RUNNING' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_run_owner_status_created
    ON cynexus_appl_ai.ai_agent_run (owner_key, status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_run_step_status_order
    ON cynexus_appl_ai.ai_agent_run_step (run_no, status, step_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_feedback_owner_created
    ON cynexus_appl_ai.ai_agent_feedback (owner_key, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_agent_usage_tenant_date
    ON cynexus_appl_ai.ai_agent_usage_daily (tenant_key, usage_date DESC);
CREATE INDEX IF NOT EXISTS ix_ai_agent_write_command_owner_status_expiry
    ON cynexus_appl_ai.ai_agent_write_command (owner_key, status, expires_at)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_ai.ai_agent_run_event IS
    'Owner-scoped append-only public workflow events for SSE replay; hidden model reasoning is excluded.';
COMMENT ON TABLE cynexus_appl_ai.ai_agent_write_audit IS
    'Owner-scoped audit trail for preview, confirmation, rejection and execution of Agent write commands.';
