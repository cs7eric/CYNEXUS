CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_daily_factor_snapshot (
    id bigint PRIMARY KEY,
    code varchar(16) NOT NULL,
    market varchar(8) NOT NULL,
    trade_date date NOT NULL,
    close numeric(20, 6),
    amount numeric(24, 2),
    turnover_pct numeric(12, 6),
    feature_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    feature_version varchar(32) NOT NULL,
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    coverage jsonb NOT NULL DEFAULT '{}'::jsonb,
    available_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_a_stock_daily_factor_snapshot UNIQUE (code, market, trade_date, feature_version)
);
CREATE INDEX IF NOT EXISTS idx_a_stock_daily_factor_snapshot_trade_code
    ON cynexus_appl_market.a_stock_daily_factor_snapshot (trade_date, code);
CREATE INDEX IF NOT EXISTS idx_a_stock_daily_factor_snapshot_code_date
    ON cynexus_appl_market.a_stock_daily_factor_snapshot (code, trade_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_membership_snapshot (
    id bigint PRIMARY KEY,
    code varchar(16) NOT NULL,
    market varchar(8) NOT NULL,
    effective_trade_date date NOT NULL,
    membership varchar(16) NOT NULL,
    industry_code varchar(64) NOT NULL,
    industry_name varchar(256),
    source_snapshot_date date NOT NULL,
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    coverage jsonb NOT NULL DEFAULT '{}'::jsonb,
    available_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_membership_snapshot UNIQUE
        (code, market, effective_trade_date, membership, industry_code),
    CONSTRAINT ck_market_factor_membership_snapshot_membership CHECK (membership IN ('ths', 'tdx'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_membership_asof
    ON cynexus_appl_market.market_factor_membership_snapshot
        (code, market, membership, effective_trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_factor_membership_industry_asof
    ON cynexus_appl_market.market_factor_membership_snapshot
        (membership, industry_code, effective_trade_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_event_intraday_snapshot (
    id bigint PRIMARY KEY,
    code varchar(16) NOT NULL,
    market varchar(8) NOT NULL,
    trade_date date NOT NULL,
    anchor_at timestamptz NOT NULL,
    minute_at timestamptz NOT NULL,
    offset_minutes integer NOT NULL,
    scope varchar(24) NOT NULL,
    membership varchar(16),
    industry_code varchar(64),
    primitive_code varchar(128) NOT NULL,
    value numeric(28, 10),
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    coverage jsonb NOT NULL DEFAULT '{}'::jsonb,
    available_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_event_intraday_snapshot UNIQUE
        (code, market, trade_date, anchor_at, minute_at, scope, membership, industry_code, primitive_code),
    CONSTRAINT ck_market_event_intraday_snapshot_scope CHECK (scope IN ('STOCK', 'MARKET', 'THS_INDUSTRY', 'TDX_INDUSTRY')),
    CONSTRAINT ck_market_event_intraday_snapshot_membership CHECK (membership IS NULL OR membership IN ('ths', 'tdx'))
);
CREATE INDEX IF NOT EXISTS idx_market_event_intraday_snapshot_event
    ON cynexus_appl_market.market_event_intraday_snapshot
        (code, market, trade_date, anchor_at, scope, minute_at);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_study (
    id bigint PRIMARY KEY,
    study_code varchar(64) NOT NULL,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    canonical_config jsonb NOT NULL,
    cohort_definition_version integer NOT NULL,
    feature_registry_version varchar(32) NOT NULL,
    data_cutoff_at timestamptz NOT NULL,
    leakage_check_status varchar(24) NOT NULL DEFAULT 'PASSED',
    status varchar(24) NOT NULL,
    progress jsonb NOT NULL DEFAULT '{}'::jsonb,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    error_code varchar(64),
    idempotency_key varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    cancelled_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    deleted_at timestamptz,
    CONSTRAINT uk_market_factor_study_code UNIQUE (study_code),
    CONSTRAINT uk_market_factor_study_owner_idempotency UNIQUE (tenant_id, owner_id, idempotency_key),
    CONSTRAINT ck_market_factor_study_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'PARTIAL', 'NO_DATA', 'FAILED', 'CANCELLED'))
    ,CONSTRAINT ck_market_factor_study_leakage CHECK (leakage_check_status IN ('PENDING', 'PASSED', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_study_owner_recent
    ON cynexus_appl_market.market_factor_study (tenant_id, owner_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_study_sample (
    id bigint PRIMARY KEY,
    study_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study(id),
    sample_type varchar(16) NOT NULL,
    stock_code varchar(16) NOT NULL,
    market varchar(8) NOT NULL,
    event_trade_date date NOT NULL,
    anchor_at timestamptz,
    event_type varchar(48) NOT NULL,
    matching_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    coverage jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_study_sample UNIQUE (study_id, sample_type, stock_code, market, event_trade_date),
    CONSTRAINT ck_market_factor_study_sample_type CHECK (sample_type IN ('EVENT', 'CONTROL'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_study_sample_study_type
    ON cynexus_appl_market.market_factor_study_sample (study_id, sample_type, event_trade_date);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_study_sequence_snapshot (
    id bigint PRIMARY KEY,
    study_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study(id),
    study_sample_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study_sample(id),
    frequency varchar(16) NOT NULL,
    sequence_code varchar(128) NOT NULL,
    offset_value integer NOT NULL,
    observed_at timestamptz NOT NULL,
    available_at timestamptz NOT NULL,
    raw_value numeric(28, 10),
    normalized_value numeric(28, 10),
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_study_sequence UNIQUE (study_sample_id, frequency, sequence_code, offset_value),
    CONSTRAINT ck_market_factor_study_sequence_frequency CHECK (frequency IN ('DAILY', 'MINUTE_1'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_study_sequence_study_code
    ON cynexus_appl_market.market_factor_study_sequence_snapshot (study_id, sequence_code, study_sample_id, offset_value);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_discovered_factor (
    id bigint PRIMARY KEY,
    discovery_code varchar(96) NOT NULL,
    recipe_version integer NOT NULL,
    source_study_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study(id),
    scope varchar(24) NOT NULL,
    discovery_method varchar(48) NOT NULL,
    validation_status varchar(24) NOT NULL,
    screenable boolean NOT NULL DEFAULT false,
    name varchar(256) NOT NULL,
    recipe jsonb NOT NULL,
    learned_bounds jsonb NOT NULL DEFAULT '{}'::jsonb,
    statistics jsonb NOT NULL DEFAULT '{}'::jsonb,
    evidence jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    retired_at timestamptz,
    CONSTRAINT uk_market_discovered_factor_version UNIQUE (discovery_code, recipe_version),
    CONSTRAINT ck_market_discovered_factor_scope CHECK (scope IN ('COMMON', 'SUBGROUP', 'RARE_SPECIAL', 'UNEXPLAINED')),
    CONSTRAINT ck_market_discovered_factor_status CHECK (validation_status IN ('GENERATED', 'EXPLORATORY', 'VALIDATED', 'PROMOTED', 'WEAKENING', 'RETIRED'))
);
CREATE INDEX IF NOT EXISTS idx_market_discovered_factor_study
    ON cynexus_appl_market.market_discovered_factor (source_study_id, validation_status, created_at DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_study_factor_result (
    id bigint PRIMARY KEY,
    study_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study(id),
    discovery_code varchar(96) NOT NULL,
    recipe_version integer NOT NULL,
    discovery_method varchar(48) NOT NULL,
    scope varchar(24) NOT NULL,
    recipe jsonb NOT NULL,
    learned_bounds jsonb NOT NULL DEFAULT '{}'::jsonb,
    statistics jsonb NOT NULL DEFAULT '{}'::jsonb,
    validation jsonb NOT NULL DEFAULT '{}'::jsonb,
    evidence jsonb NOT NULL DEFAULT '{}'::jsonb,
    verdict varchar(24) NOT NULL,
    rank integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_study_factor_result UNIQUE (study_id, discovery_code, recipe_version),
    CONSTRAINT ck_market_factor_study_factor_result_scope CHECK (scope IN ('COMMON', 'SUBGROUP', 'RARE_SPECIAL', 'UNEXPLAINED')),
    CONSTRAINT ck_market_factor_study_factor_result_verdict CHECK (verdict IN ('EXPLORATORY', 'CANDIDATE', 'WEAK', 'REJECTED', 'UNAVAILABLE'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_study_factor_result_study
    ON cynexus_appl_market.market_factor_study_factor_result (study_id, rank, created_at DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_pattern_cluster (
    id bigint PRIMARY KEY,
    study_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_study(id),
    cluster_code varchar(96) NOT NULL,
    scope varchar(24) NOT NULL,
    discovery_method varchar(48) NOT NULL,
    centroid jsonb NOT NULL DEFAULT '{}'::jsonb,
    representative_samples jsonb NOT NULL DEFAULT '[]'::jsonb,
    regime_distribution jsonb NOT NULL DEFAULT '{}'::jsonb,
    quality jsonb NOT NULL DEFAULT '{}'::jsonb,
    member_count integer NOT NULL,
    control_count integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_pattern_cluster UNIQUE (study_id, cluster_code),
    CONSTRAINT ck_market_factor_pattern_cluster_scope CHECK (scope IN ('COMMON', 'SUBGROUP', 'RARE_SPECIAL', 'UNEXPLAINED'))
);
CREATE INDEX IF NOT EXISTS idx_market_factor_pattern_cluster_study
    ON cynexus_appl_market.market_factor_pattern_cluster (study_id, scope, created_at DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_set_definition (
    id bigint PRIMARY KEY,
    factor_set_code varchar(64) NOT NULL,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    name varchar(128) NOT NULL,
    status varchar(24) NOT NULL,
    active_version_id bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_market_factor_set_definition_code UNIQUE (factor_set_code),
    CONSTRAINT ck_market_factor_set_definition_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'))
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_factor_set_version (
    id bigint PRIMARY KEY,
    factor_set_definition_id bigint NOT NULL REFERENCES cynexus_appl_market.market_factor_set_definition(id),
    version_no integer NOT NULL,
    source_study_id bigint REFERENCES cynexus_appl_market.market_factor_study(id),
    expression jsonb NOT NULL,
    validation_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(24) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_factor_set_version UNIQUE (factor_set_definition_id, version_no),
    CONSTRAINT ck_market_factor_set_version_status CHECK (status IN ('DRAFT', 'VALIDATED', 'PROMOTED', 'RETIRED'))
);
ALTER TABLE cynexus_appl_market.market_factor_set_definition
    DROP CONSTRAINT IF EXISTS fk_market_factor_set_definition_active_version;
ALTER TABLE cynexus_appl_market.market_factor_set_definition
    ADD CONSTRAINT fk_market_factor_set_definition_active_version
    FOREIGN KEY (active_version_id) REFERENCES cynexus_appl_market.market_factor_set_version(id);

ALTER TABLE cynexus_appl_market.stock_selection_strategy_version
    ADD COLUMN IF NOT EXISTS source_factor_study_id bigint REFERENCES cynexus_appl_market.market_factor_study(id),
    ADD COLUMN IF NOT EXISTS source_factor_set_version_id bigint REFERENCES cynexus_appl_market.market_factor_set_version(id);
ALTER TABLE cynexus_appl_market.stock_selection_strategy_candidate
    ADD COLUMN IF NOT EXISTS factor_set_version_id bigint REFERENCES cynexus_appl_market.market_factor_set_version(id),
    ADD COLUMN IF NOT EXISTS source_study_id bigint REFERENCES cynexus_appl_market.market_factor_study(id);

COMMENT ON TABLE cynexus_appl_market.market_factor_study IS 'Owner-scoped immutable market-event research runs. Leakage failure is terminal FAILED.';
COMMENT ON TABLE cynexus_appl_market.market_discovered_factor IS 'Versioned deterministic sequence discovery. Only PROMOTED entries may be screenable.';
