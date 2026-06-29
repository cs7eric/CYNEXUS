CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_industry_application_config (
    id BIGINT NOT NULL PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    horizon_days INTEGER NOT NULL DEFAULT 120,
    horizon_segments INTEGER NOT NULL DEFAULT 4,
    source VARCHAR(128),
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_industry_application_target (
    id BIGINT NOT NULL PRIMARY KEY,
    target_key VARCHAR(128) NOT NULL UNIQUE,
    target_type VARCHAR(32) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    interval_minutes INTEGER NOT NULL DEFAULT 60,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    display_order INTEGER NOT NULL DEFAULT 0,
    source VARCHAR(128),
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_mkt_ind_app_target_type
    ON cynexus_appl_market.mkt_industry_application_target(target_type)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_mkt_ind_app_target_enabled
    ON cynexus_appl_market.mkt_industry_application_target(enabled, display_order)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_industry_application_result (
    id BIGINT NOT NULL PRIMARY KEY,
    target_key VARCHAR(128) NOT NULL UNIQUE,
    target_type VARCHAR(32) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    updated_at_result TIMESTAMPTZ NOT NULL,
    kline JSONB NOT NULL DEFAULT '[]'::jsonb,
    indicators JSONB NOT NULL DEFAULT '{}'::jsonb,
    meta JSONB NOT NULL DEFAULT '{}'::jsonb,
    source VARCHAR(128),
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_mkt_ind_app_result_symbol
    ON cynexus_appl_market.mkt_industry_application_result(symbol)
    WHERE deleted_at IS NULL;

INSERT INTO cynexus_appl_market.mkt_industry_application_config (
    id, config_key, version, horizon_days, horizon_segments, source, created_at, updated_at
)
VALUES (
    202606290001, 'default', 1, 120, 4, 'migration:legacy-reference', now(), now()
)
ON CONFLICT (config_key) DO UPDATE SET
    horizon_days = EXCLUDED.horizon_days,
    horizon_segments = EXCLUDED.horizon_segments,
    source = EXCLUDED.source,
    updated_at = now(),
    deleted_at = NULL;

INSERT INTO cynexus_appl_market.mkt_industry_application_target (
    id, target_key, target_type, symbol, name, enabled, interval_minutes, tags, display_order, source, created_at, updated_at
)
VALUES
    (202606290101, 'industry-sh880301', 'industry', 'sh880301', '农副食品', TRUE, 60, '[]'::jsonb, 0, 'migration:legacy-reference', now(), now()),
    (202606290102, 'concept-sh880401', 'concept', 'sh880401', '沪深300概念', TRUE, 60, '[]'::jsonb, 1, 'migration:legacy-reference', now(), now()),
    (202606290103, 'concept-sh880427', 'concept', 'sh880427', '智能制造', TRUE, 60, '["manual"]'::jsonb, 2, 'migration:legacy-reference', now(), now()),
    (202606290104, 'concept-sh880424', 'concept', 'sh880424', '半导体', TRUE, 60, '["manual"]'::jsonb, 3, 'migration:legacy-reference', now(), now()),
    (202606290105, 'industry-sh880307', 'industry', 'sh880307', '房地产', TRUE, 60, '["manual"]'::jsonb, 4, 'migration:legacy-reference', now(), now()),
    (202606290106, 'concept-sh880450', 'concept', 'sh880450', '创新药', TRUE, 60, '["manual"]'::jsonb, 5, 'migration:legacy-reference', now(), now()),
    (202606290107, 'concept-sh880448', 'concept', 'sh880448', '乡村振兴', TRUE, 60, '["manual"]'::jsonb, 6, 'migration:legacy-reference', now(), now()),
    (202606290108, 'concept-sh880435', 'concept', 'sh880435', '固态电池', TRUE, 60, '["manual"]'::jsonb, 7, 'migration:legacy-reference', now(), now()),
    (202606290109, 'concept-sh880405', 'concept', 'sh880405', '红利概念', TRUE, 60, '["manual"]'::jsonb, 8, 'migration:legacy-reference', now(), now()),
    (202606290110, 'concept-sh880422', 'concept', 'sh880422', '国产芯片', TRUE, 60, '["manual"]'::jsonb, 9, 'migration:legacy-reference', now(), now()),
    (202606290111, 'concept-sh880431', 'concept', 'sh880431', 'CPO', TRUE, 60, '["manual"]'::jsonb, 10, 'migration:legacy-reference', now(), now()),
    (202606290112, 'concept-sh880409', 'concept', 'sh880409', '一带一路', TRUE, 60, '["manual"]'::jsonb, 11, 'migration:legacy-reference', now(), now())
ON CONFLICT (target_key) DO UPDATE SET
    target_type = EXCLUDED.target_type,
    symbol = EXCLUDED.symbol,
    name = EXCLUDED.name,
    enabled = EXCLUDED.enabled,
    interval_minutes = EXCLUDED.interval_minutes,
    tags = EXCLUDED.tags,
    display_order = EXCLUDED.display_order,
    source = EXCLUDED.source,
    updated_at = now(),
    deleted_at = NULL;
