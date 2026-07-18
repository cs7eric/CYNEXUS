\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE cynexus_appl_ai.ai_usage_bindings
    ADD COLUMN IF NOT EXISTS prompt_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_ai_usage_bindings_prompt'
           AND conrelid = 'cynexus_appl_ai.ai_usage_bindings'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_ai.ai_usage_bindings
            ADD CONSTRAINT fk_ai_usage_bindings_prompt
            FOREIGN KEY (prompt_id) REFERENCES cynexus_appl_ai.ai_prompt(id);
    END IF;
END $$;

COMMIT;
