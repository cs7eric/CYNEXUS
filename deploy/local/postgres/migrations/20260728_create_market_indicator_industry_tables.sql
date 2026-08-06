CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_industry_snapshots (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    captured_at timestamptz NOT NULL,
    industry_code text,
    industry_name text NOT NULL,
    stock_count integer NOT NULL DEFAULT 0,
    advancing_count integer NOT NULL DEFAULT 0,
    declining_count integer NOT NULL DEFAULT 0,
    flat_count integer NOT NULL DEFAULT 0,
    limit_up_count integer NOT NULL DEFAULT 0,
    limit_up_touch_count integer NOT NULL DEFAULT 0,
    limit_down_count integer NOT NULL DEFAULT 0,
    limit_down_touch_count integer NOT NULL DEFAULT 0,
    rapid_rise_count integer NOT NULL DEFAULT 0,
    rapid_fall_count integer NOT NULL DEFAULT 0,
    first_limit_up_at timestamptz,
    first_limit_up_code varchar(6),
    first_limit_up_name text,
    first_limit_up_change_pct numeric(12,6),
    first_event_at timestamptz,
    first_event_code varchar(6),
    first_event_name text,
    first_event_type varchar(32),
    first_event_signal_value numeric(20,6),
    source text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_indicator_industry_capture
    ON cynexus_appl_market.market_indicator_industry_snapshots (trade_date, captured_at, industry_name)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_market_indicator_industry_latest
    ON cynexus_appl_market.market_indicator_industry_snapshots (trade_date, captured_at DESC, limit_up_count DESC)
    WHERE deleted_at IS NULL;
