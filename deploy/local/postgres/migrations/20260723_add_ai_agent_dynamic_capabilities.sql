ALTER TABLE cynexus_appl_ai.ai_agent_capability
    ADD COLUMN IF NOT EXISTS registration_mode varchar(32) NOT NULL DEFAULT 'HANDLER';

UPDATE cynexus_appl_ai.ai_agent_capability
SET registration_mode = 'HANDLER'
WHERE registration_mode IS NULL;

ALTER TABLE cynexus_appl_ai.ai_agent_capability
    DROP CONSTRAINT IF EXISTS ck_ai_agent_capability_registration_mode;

ALTER TABLE cynexus_appl_ai.ai_agent_capability
    ADD CONSTRAINT ck_ai_agent_capability_registration_mode
    CHECK (registration_mode IN ('HANDLER', 'ADMIN'));

CREATE INDEX IF NOT EXISTS idx_ai_agent_capability_registration_mode
    ON cynexus_appl_ai.ai_agent_capability (registration_mode, capability_code, version);
