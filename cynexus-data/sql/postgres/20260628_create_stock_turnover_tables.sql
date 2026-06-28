CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.stock_turnover_series_snapshot
(
    symbol text NOT NULL,
    target_type text NOT NULL DEFAULT 'stock',
    period text NOT NULL DEFAULT '1d',
    adjust text NOT NULL DEFAULT 'qfq',
    circulating_shares numeric(24,6),
    total_shares numeric(24,6),
    latest_trade_date date,
    latest_turnover_rate numeric(18,6),
    entry_count integer NOT NULL DEFAULT 0,
    source text NOT NULL DEFAULT 'clickhouse+eltdx',
    payload jsonb NOT NULL,
    generated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (symbol, target_type, period, adjust, source)
);

CREATE INDEX IF NOT EXISTS idx_stock_turnover_series_snapshot_alive
    ON cynexus_data.stock_turnover_series_snapshot (symbol, target_type, latest_trade_date DESC)
    WHERE deleted_at IS NULL;
