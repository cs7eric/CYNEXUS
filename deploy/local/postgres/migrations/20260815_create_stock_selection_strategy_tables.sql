CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_definition (
    id bigint PRIMARY KEY,
    strategy_code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    description text NOT NULL,
    strategy_type varchar(32) NOT NULL,
    status varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_stock_selection_strategy_definition_code UNIQUE (strategy_code),
    CONSTRAINT ck_stock_selection_strategy_definition_type CHECK (strategy_type IN ('SCREENER_PRESET')),
    CONSTRAINT ck_stock_selection_strategy_definition_status CHECK (status IN ('ACTIVE', 'RETIRED'))
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_version (
    id bigint PRIMARY KEY,
    strategy_definition_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_definition(id),
    version_no integer NOT NULL,
    status varchar(32) NOT NULL,
    parameter_schema jsonb NOT NULL,
    evidence_schema jsonb NOT NULL,
    change_summary text NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_stock_selection_strategy_version UNIQUE (strategy_definition_id, version_no),
    CONSTRAINT ck_stock_selection_strategy_version_status CHECK (status IN ('ACTIVE', 'RETIRED')),
    CONSTRAINT ck_stock_selection_strategy_version_window CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_run (
    id bigint PRIMARY KEY,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    strategy_version_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_version(id),
    requested_trade_date date,
    actual_trade_date date,
    status varchar(32) NOT NULL,
    parameter_snapshot jsonb NOT NULL,
    data_source text NOT NULL,
    data_freshness jsonb NOT NULL DEFAULT '{}'::jsonb,
    universe_count integer NOT NULL DEFAULT 0,
    candidate_count integer NOT NULL DEFAULT 0,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    started_at timestamptz NOT NULL,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_stock_selection_strategy_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'NO_DATA', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_stock_selection_strategy_run_counts CHECK (universe_count >= 0 AND candidate_count >= 0)
);
CREATE INDEX IF NOT EXISTS idx_stock_selection_strategy_run_owner_recent
    ON cynexus_appl_market.stock_selection_strategy_run (tenant_id, owner_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_candidate (
    id bigint PRIMARY KEY,
    strategy_run_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_run(id),
    stock_code varchar(16) NOT NULL,
    stock_name varchar(128),
    rank_no integer NOT NULL,
    score numeric(18, 6) NOT NULL,
    reason_summary text NOT NULL,
    snapshot jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_selection_strategy_candidate_run_stock UNIQUE (strategy_run_id, stock_code),
    CONSTRAINT uk_stock_selection_strategy_candidate_run_rank UNIQUE (strategy_run_id, rank_no),
    CONSTRAINT ck_stock_selection_strategy_candidate_rank CHECK (rank_no > 0)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_candidate_evidence (
    id bigint PRIMARY KEY,
    strategy_candidate_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_candidate(id),
    evidence_code varchar(64) NOT NULL,
    label varchar(128) NOT NULL,
    operator varchar(16) NOT NULL,
    expected_value jsonb NOT NULL,
    actual_value jsonb NOT NULL,
    matched boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_selection_strategy_candidate_evidence UNIQUE (strategy_candidate_id, evidence_code)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_watchlist_item (
    id bigint PRIMARY KEY,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    stock_code varchar(16) NOT NULL,
    stock_name varchar(128),
    source_strategy_candidate_id bigint REFERENCES cynexus_appl_market.stock_selection_strategy_candidate(id),
    status varchar(32) NOT NULL,
    attention_reason text NOT NULL,
    invalidation_condition text,
    next_review_date date,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_stock_selection_watchlist_item_status CHECK (status IN ('WATCHING', 'PAUSED', 'INVALIDATED', 'ARCHIVED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_selection_watchlist_active_owner_stock
    ON cynexus_appl_market.stock_selection_watchlist_item (tenant_id, owner_id, stock_code)
    WHERE deleted_at IS NULL AND status IN ('WATCHING', 'PAUSED');
CREATE INDEX IF NOT EXISTS idx_stock_selection_watchlist_owner_status
    ON cynexus_appl_market.stock_selection_watchlist_item (tenant_id, owner_id, status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_watchlist_review (
    id bigint PRIMARY KEY,
    watchlist_item_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_watchlist_item(id),
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    decision varchar(32) NOT NULL,
    note text,
    reviewed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_selection_watchlist_review_decision CHECK (decision IN ('CONTINUE', 'PAUSE', 'INVALIDATE', 'ARCHIVE'))
);
CREATE INDEX IF NOT EXISTS idx_stock_selection_watchlist_review_item
    ON cynexus_appl_market.stock_selection_watchlist_review (watchlist_item_id, reviewed_at DESC);

INSERT INTO cynexus_appl_market.stock_selection_strategy_definition
    (id, strategy_code, name, description, strategy_type, status)
VALUES
    (915000000000000101, 'TREND_MOMENTUM', '趋势动量', '日频趋势、量能和流动性共同确认的收盘后候选。', 'SCREENER_PRESET', 'ACTIVE'),
    (915000000000000102, 'VOLUME_CAPITAL_RESONANCE', '放量资金共振', '以成交活跃度、量比和主力净流入共同确认的收盘后候选。', 'SCREENER_PRESET', 'ACTIVE'),
    (915000000000000103, 'SECTOR_BACKED_MOMENTUM', '板块归属动量', '具备可追溯板块归属，且自身趋势与流动性同时达标的收盘后候选；不将行业归属误表述为行业强度。', 'SCREENER_PRESET', 'ACTIVE')
ON CONFLICT (strategy_code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    strategy_type = EXCLUDED.strategy_type,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO cynexus_appl_market.stock_selection_strategy_version
    (id, strategy_definition_id, version_no, status, parameter_schema, evidence_schema, change_summary, effective_from)
VALUES
    (915000000000000201, 915000000000000101, 1, 'ACTIVE',
     '{"universe":"ALL","excludeSt":true,"minReturnPct10d":5,"minReturnPct30d":10,"minTurnoverPct":1,"minVolumeRatio":1,"sort":"return_30d","sortDir":"desc"}'::jsonb,
     '["returnPct10d","returnPct30d","turnoverPct","volumeRatio"]'::jsonb,
     'Initial governed daily-close momentum preset.', DATE '2026-08-15'),
    (915000000000000202, 915000000000000102, 1, 'ACTIVE',
     '{"universe":"ALL","excludeSt":true,"minAmount":100000000,"minMainNet":0,"minTurnoverPct":1,"minVolumeRatio":1.2,"sort":"main_net","sortDir":"desc"}'::jsonb,
     '["amount","mainNet","turnoverPct","volumeRatio"]'::jsonb,
     'Initial governed daily-close volume and capital preset.', DATE '2026-08-15'),
    (915000000000000203, 915000000000000103, 1, 'ACTIVE',
     '{"universe":"ALL","excludeSt":true,"requiresTdxIndustry":true,"minReturnPct5d":2,"minReturnPct10d":5,"minTurnoverPct":1,"minVolumeRatio":1,"sort":"return_10d","sortDir":"desc"}'::jsonb,
     '["tdxIndustry","returnPct5d","returnPct10d","turnoverPct","volumeRatio"]'::jsonb,
     'Initial governed daily-close sector-membership and momentum preset.', DATE '2026-08-15')
ON CONFLICT (strategy_definition_id, version_no) DO UPDATE
SET status = EXCLUDED.status,
    parameter_schema = EXCLUDED.parameter_schema,
    evidence_schema = EXCLUDED.evidence_schema,
    change_summary = EXCLUDED.change_summary,
    updated_at = now();
