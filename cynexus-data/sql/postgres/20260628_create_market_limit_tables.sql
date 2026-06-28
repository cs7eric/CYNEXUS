CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.market_limit_stock_snapshot
(
    trade_date date NOT NULL,
    code text NOT NULL,
    name text,
    category text NOT NULL,
    streak integer,
    change_pct numeric(12,6),
    limit_up_price numeric(20,6),
    limit_down_price numeric(20,6),
    close numeric(20,6),
    source text NOT NULL DEFAULT 'clickhouse.daily_raw',
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (trade_date, code, category, source)
);

CREATE INDEX IF NOT EXISTS idx_market_limit_stock_snapshot_date_category_alive
    ON cynexus_data.market_limit_stock_snapshot (trade_date DESC, category, streak DESC NULLS LAST)
    WHERE deleted_at IS NULL;
