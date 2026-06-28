CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.market_style_sector_snapshot
(
    trade_date date NOT NULL,
    name text NOT NULL,
    rank_by_change integer,
    change_pct numeric(12,6),
    valid_size integer NOT NULL DEFAULT 0,
    sample_size integer NOT NULL DEFAULT 0,
    source text NOT NULL DEFAULT 'tencent.qt',
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (trade_date, name, source)
);

CREATE INDEX IF NOT EXISTS idx_market_style_sector_snapshot_date_rank_alive
    ON cynexus_data.market_style_sector_snapshot (trade_date DESC, rank_by_change ASC)
    WHERE deleted_at IS NULL;
