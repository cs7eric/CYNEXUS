\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS cynexus_appl_ai;

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_prompt (
    id bigint PRIMARY KEY,
    prompt_key varchar(128) NOT NULL,
    name varchar(128) NOT NULL,
    scenario_code varchar(128),
    prompt_version varchar(64) NOT NULL DEFAULT 'v1',
    status varchar(32) NOT NULL DEFAULT 'draft',
    system_prompt text NOT NULL,
    user_prompt_template text,
    output_schema jsonb,
    variables jsonb NOT NULL DEFAULT '[]'::jsonb,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_ai_prompt_status CHECK (status IN ('draft', 'active', 'archived'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_prompt_key_version_alive
    ON cynexus_appl_ai.ai_prompt (prompt_key, prompt_version)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_prompt_scenario_alive
    ON cynexus_appl_ai.ai_prompt (scenario_code, status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_prompt_status_alive
    ON cynexus_appl_ai.ai_prompt (status, updated_at DESC)
    WHERE deleted_at IS NULL;

COMMIT;
