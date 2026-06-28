CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.market_heatmap_snapshot
(
    snapshot_date date NOT NULL,
    kind text NOT NULL,
    source text NOT NULL,
    payload jsonb NOT NULL,
    item_count integer NOT NULL DEFAULT 0,
    generated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (snapshot_date, kind, source)
);

CREATE INDEX IF NOT EXISTS idx_market_heatmap_snapshot_alive
    ON cynexus_data.market_heatmap_snapshot (snapshot_date DESC, kind, source)
    WHERE deleted_at IS NULL;
