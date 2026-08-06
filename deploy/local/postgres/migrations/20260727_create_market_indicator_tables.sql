CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_snapshots (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(6) NOT NULL,
    name text,
    market varchar(8),
    last_price numeric(20,6),
    prev_close numeric(20,6),
    open_price numeric(20,6),
    high_price numeric(20,6),
    low_price numeric(20,6),
    change_pct numeric(12,6),
    amount numeric(24,6),
    volume numeric(24,6),
    turnover_rate numeric(12,6),
    limit_up_price numeric(20,6),
    limit_down_price numeric(20,6),
    limit_status varchar(32) NOT NULL DEFAULT 'normal',
    source text NOT NULL,
    captured_at timestamptz NOT NULL,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_market_indicator_snapshots_latest
    ON cynexus_appl_market.market_indicator_snapshots (trade_date, code, captured_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_market_indicator_snapshots_status
    ON cynexus_appl_market.market_indicator_snapshots (trade_date, limit_status, captured_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_events (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(6) NOT NULL,
    name text,
    market varchar(8),
    event_type varchar(32) NOT NULL,
    event_at timestamptz NOT NULL,
    event_minute timestamptz NOT NULL,
    signal_value numeric(20,6),
    last_price numeric(20,6),
    change_pct numeric(12,6),
    limit_status varchar(32),
    source text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_market_indicator_events_key
    ON cynexus_appl_market.market_indicator_events (trade_date, code, event_type, event_minute)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_market_indicator_events_latest
    ON cynexus_appl_market.market_indicator_events (trade_date, event_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_candidate_pool (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    captured_at timestamptz NOT NULL,
    code varchar(6) NOT NULL,
    pool_tier varchar(32) NOT NULL,
    reason varchar(64) NOT NULL,
    priority_score numeric(20,6),
    source text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_market_indicator_candidate_pool_key
    ON cynexus_appl_market.market_indicator_candidate_pool (trade_date, captured_at, code)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_market_indicator_candidate_pool_latest
    ON cynexus_appl_market.market_indicator_candidate_pool (trade_date, captured_at DESC, pool_tier)
    WHERE deleted_at IS NULL;
