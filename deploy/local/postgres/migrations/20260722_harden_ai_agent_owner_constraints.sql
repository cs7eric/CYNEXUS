-- Repair owner-qualified relationships for databases created before the Agent persistence contract.
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_conversation_owner_no
    ON cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_run_owner_no
    ON cynexus_appl_ai.ai_agent_run (owner_key, run_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_run_owner_idempotency
    ON cynexus_appl_ai.ai_agent_run (owner_key, idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;

DO $$
BEGIN
    IF to_regclass('cynexus_appl_ai.ai_agent_run_step') IS NOT NULL THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step
            DROP CONSTRAINT IF EXISTS fk_ai_agent_run_step_run;
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step
            DROP CONSTRAINT IF EXISTS fk_ai_agent_run_step_owner_run;
        ALTER TABLE cynexus_appl_ai.ai_agent_run_step
            ADD CONSTRAINT fk_ai_agent_run_step_owner_run
            FOREIGN KEY (owner_key, run_no)
            REFERENCES cynexus_appl_ai.ai_agent_run (owner_key, run_no);
    END IF;
    IF to_regclass('cynexus_appl_ai.ai_agent_message') IS NOT NULL THEN
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            DROP CONSTRAINT IF EXISTS fk_ai_agent_message_conversation;
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            DROP CONSTRAINT IF EXISTS fk_ai_agent_message_owner_conversation;
        ALTER TABLE cynexus_appl_ai.ai_agent_message
            ADD CONSTRAINT fk_ai_agent_message_owner_conversation
            FOREIGN KEY (owner_key, conversation_no)
            REFERENCES cynexus_appl_ai.ai_agent_conversation (owner_key, conversation_no);
    END IF;
END $$;

ALTER TABLE cynexus_appl_ai.ai_agent_task
    DROP CONSTRAINT IF EXISTS ck_ai_agent_task_lease_shape;
ALTER TABLE cynexus_appl_ai.ai_agent_task
    ADD CONSTRAINT ck_ai_agent_task_lease_shape CHECK (
        (status = 'RUNNING' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL)
        OR status <> 'RUNNING'
    );
