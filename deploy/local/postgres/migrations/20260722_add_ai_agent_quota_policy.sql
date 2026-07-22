CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_agent_quota_policy (
    id bigint PRIMARY KEY,
    tenant_key varchar(128),
    principal_key varchar(128),
    daily_request_limit integer NOT NULL DEFAULT 200,
    daily_token_limit bigint NOT NULL DEFAULT 100000,
    enabled boolean NOT NULL DEFAULT true,
    effective_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_ai_agent_quota_policy_scope CHECK (tenant_key IS NOT NULL OR principal_key IS NULL),
    CONSTRAINT ck_ai_agent_quota_policy_limits CHECK (daily_request_limit > 0 AND daily_token_limit >= 1024)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_agent_quota_policy_scope
    ON cynexus_appl_ai.ai_agent_quota_policy (COALESCE(tenant_key, ''), COALESCE(principal_key, ''));
CREATE INDEX IF NOT EXISTS ix_ai_agent_quota_policy_lookup
    ON cynexus_appl_ai.ai_agent_quota_policy (tenant_key, principal_key, enabled, effective_at DESC);

INSERT INTO cynexus_appl_ai.ai_agent_quota_policy
    (id, tenant_key, principal_key, daily_request_limit, daily_token_limit, enabled)
VALUES (170000000000000001, NULL, NULL, 200, 100000, true)
ON CONFLICT DO NOTHING;
