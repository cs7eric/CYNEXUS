\set ON_ERROR_STOP on

DO $$
DECLARE
    required text[] := ARRAY[
        'ai_agent_conversation', 'ai_agent_run', 'ai_agent_run_step',
        'ai_agent_message', 'ai_agent_run_event', 'ai_agent_task',
        'ai_agent_usage_daily', 'ai_agent_feedback', 'ai_agent_write_command',
        'ai_agent_write_audit', 'ai_agent_capability', 'ai_agent_capability_audit'
    ];
    item text;
BEGIN
    FOREACH item IN ARRAY required LOOP
        IF to_regclass('cynexus_appl_ai.' || item) IS NULL THEN
            RAISE EXCEPTION 'Missing Agent table: cynexus_appl_ai.%', item;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    bad_count bigint;
BEGIN
    SELECT count(*) INTO bad_count
    FROM information_schema.columns
    WHERE table_schema = 'cynexus_appl_ai'
      AND table_name IN ('ai_agent_conversation', 'ai_agent_run', 'ai_agent_run_event',
                         'ai_agent_task', 'ai_agent_feedback', 'ai_agent_usage_daily',
                         'ai_agent_write_command', 'ai_agent_write_audit')
      AND column_name = 'owner_key'
      AND (data_type <> 'character varying' OR character_maximum_length <> 128);
    IF bad_count <> 0 THEN
        RAISE EXCEPTION 'Owner key contract mismatch in % Agent tables', bad_count;
    END IF;
END $$;

DO $$
DECLARE
    required_constraints text[] := ARRAY[
        'fk_ai_agent_run_conversation', 'fk_ai_agent_message_conversation',
        'fk_ai_agent_message_run', 'fk_ai_agent_run_step_run',
        'fk_ai_agent_run_event_run_owner', 'fk_ai_agent_task_conversation',
        'fk_ai_agent_task_run', 'fk_ai_agent_feedback_run_owner',
        'fk_ai_agent_feedback_conversation', 'fk_ai_agent_write_command_run',
        'fk_ai_agent_write_command_conversation', 'fk_ai_agent_write_audit_command_owner'
    ];
    item text;
BEGIN
    FOREACH item IN ARRAY required_constraints LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = item) THEN
            RAISE EXCEPTION 'Missing Agent persistence constraint: %', item;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    orphans bigint;
BEGIN
    SELECT count(*) INTO orphans
    FROM cynexus_appl_ai.ai_agent_run_step s
    LEFT JOIN cynexus_appl_ai.ai_agent_run r ON r.run_no = s.run_no
    WHERE r.run_no IS NULL;
    IF orphans <> 0 THEN RAISE EXCEPTION 'Orphan Agent run steps: %', orphans; END IF;

    SELECT count(*) INTO orphans
    FROM cynexus_appl_ai.ai_agent_message m
    LEFT JOIN cynexus_appl_ai.ai_agent_conversation c ON c.conversation_no = m.conversation_no
    WHERE c.conversation_no IS NULL;
    IF orphans <> 0 THEN RAISE EXCEPTION 'Orphan Agent messages: %', orphans; END IF;

    SELECT count(*) INTO orphans
    FROM cynexus_appl_ai.ai_agent_run_event e
    LEFT JOIN cynexus_appl_ai.ai_agent_run r
      ON r.run_no = e.run_no AND r.owner_key = e.owner_key
    WHERE r.run_no IS NULL;
    IF orphans <> 0 THEN RAISE EXCEPTION 'Orphan or cross-owner Agent events: %', orphans; END IF;
END $$;

SELECT 'Agent persistence contract checks passed' AS result;
