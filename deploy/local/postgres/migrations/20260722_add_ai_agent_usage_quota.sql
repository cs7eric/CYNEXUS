ALTER TABLE cynexus_appl_ai.ai_agent_run
    ADD COLUMN IF NOT EXISTS input_tokens integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS output_tokens integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_tokens integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS estimated_cost numeric(18,8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS usage_source varchar(16) NOT NULL DEFAULT 'ESTIMATED';

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_usage_daily (
    owner_key varchar(128) NOT NULL,
    usage_date date NOT NULL,
    tenant_key varchar(128) NOT NULL,
    principal_key varchar(128) NOT NULL,
    request_count integer NOT NULL DEFAULT 0,
    reserved_tokens bigint NOT NULL DEFAULT 0,
    input_tokens bigint NOT NULL DEFAULT 0,
    output_tokens bigint NOT NULL DEFAULT 0,
    total_tokens bigint NOT NULL DEFAULT 0,
    estimated_cost numeric(18,8) NOT NULL DEFAULT 0,
    estimated_usage_count integer NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (owner_key, usage_date)
);
