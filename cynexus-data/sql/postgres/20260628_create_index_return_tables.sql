CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.mkt_index_return_daily
(
    trade_date date NOT NULL,
    index_code text NOT NULL,
    index_name text NOT NULL,
    window_days integer NOT NULL,
    current_close numeric(20,6),
    current_trade_date date,
    base_close numeric(20,6),
    base_date date,
    return_pct numeric(12,6),
    source text NOT NULL DEFAULT 'clickhouse.index_daily_raw',
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (trade_date, index_code, window_days)
);

CREATE INDEX IF NOT EXISTS idx_mkt_index_return_daily_alive
    ON cynexus_data.mkt_index_return_daily (trade_date DESC, window_days, index_code)
    WHERE deleted_at IS NULL;
