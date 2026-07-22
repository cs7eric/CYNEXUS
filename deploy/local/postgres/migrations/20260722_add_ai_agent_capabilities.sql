CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_capability (
    id bigint PRIMARY KEY,
    capability_code varchar(160) NOT NULL,
    version varchar(32) NOT NULL,
    name varchar(255) NOT NULL,
    module varchar(100) NOT NULL,
    description varchar(2000) NOT NULL,
    input_schema_json jsonb NOT NULL,
    output_schema_json jsonb NOT NULL,
    is_read_only boolean NOT NULL DEFAULT true,
    requires_confirmation boolean NOT NULL DEFAULT false,
    tenant_scope varchar(32) NOT NULL DEFAULT 'TENANT',
    source varchar(255),
    source_url varchar(1000),
    freshness varchar(255),
    availability varchar(32) NOT NULL DEFAULT 'AVAILABLE',
    governance_availability varchar(32) NOT NULL DEFAULT 'AVAILABLE',
    is_enabled boolean NOT NULL DEFAULT true,
    priority integer NOT NULL DEFAULT 100,
    prompt_key varchar(160) NOT NULL,
    runtime_entry varchar(500) NOT NULL,
    route_group varchar(255) NOT NULL,
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    stale_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_ai_agent_capability_code_version UNIQUE (capability_code, version)
);
ALTER TABLE cynexus_appl_ai.ai_agent_capability
    ADD COLUMN IF NOT EXISTS governance_availability varchar(32) NOT NULL DEFAULT 'AVAILABLE';
CREATE INDEX IF NOT EXISTS idx_ai_agent_capability_governance
    ON cynexus_appl_ai.ai_agent_capability (governance_availability, is_enabled, priority);

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_capability_audit (
    id bigint PRIMARY KEY,
    capability_code varchar(160) NOT NULL,
    version varchar(32),
    action varchar(64) NOT NULL,
    enabled boolean,
    availability varchar(32),
    governance_availability varchar(32),
    priority integer,
    reason varchar(500),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_ai_agent_capability_audit_code_time
    ON cynexus_appl_ai.ai_agent_capability_audit (capability_code, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_agent_capability_runtime
    ON cynexus_appl_ai.ai_agent_capability (is_enabled, availability, priority, capability_code);
