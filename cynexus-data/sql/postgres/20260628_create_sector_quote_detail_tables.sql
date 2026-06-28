CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.tdx_sector_quote_detail_snapshot
(
    id bigint PRIMARY KEY,
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
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdx_sector_quote_detail_date_code_source_alive
    ON cynexus_appl_market.tdx_sector_quote_detail_snapshot (snapshot_date, code6, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tdx_sector_quote_detail_alive
    ON cynexus_appl_market.tdx_sector_quote_detail_snapshot (snapshot_date DESC, code6, source)
    WHERE deleted_at IS NULL;
