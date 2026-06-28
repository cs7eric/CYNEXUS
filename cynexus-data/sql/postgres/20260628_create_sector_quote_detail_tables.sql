CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.tdx_sector_quote_detail_snapshot
(
    snapshot_date date NOT NULL,
    code6 text NOT NULL,
    name text NOT NULL,
    seed_code text,
    source text NOT NULL DEFAULT 'eltdx.f10.theme_market',
    payload jsonb NOT NULL,
    section_counts jsonb NOT NULL DEFAULT '{}'::jsonb,
    generated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (snapshot_date, code6, source)
);

CREATE INDEX IF NOT EXISTS idx_tdx_sector_quote_detail_alive
    ON cynexus_data.tdx_sector_quote_detail_snapshot (snapshot_date DESC, code6, source)
    WHERE deleted_at IS NULL;
