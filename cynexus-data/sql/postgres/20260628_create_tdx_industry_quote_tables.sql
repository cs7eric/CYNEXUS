CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.tdx_industry_quote_batch
(
    id bigint PRIMARY KEY,
    batch_key text NOT NULL,
    snapshot_date date NOT NULL,
    source text NOT NULL DEFAULT 'eltdx',
    row_count integer NOT NULL DEFAULT 0,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdx_industry_quote_batch_key_alive
    ON cynexus_appl_market.tdx_industry_quote_batch (batch_key)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.tdx_industry_quote_snapshot
(
    id bigint PRIMARY KEY,
    batch_id bigint NOT NULL REFERENCES cynexus_appl_market.tdx_industry_quote_batch(id),
    snapshot_date date NOT NULL,
    code6 text NOT NULL,
    name text NOT NULL,
    full_code text NOT NULL,
    exchange text,
    last_price numeric(20,6),
    pre_close_price numeric(20,6),
    open_price numeric(20,6),
    high_price numeric(20,6),
    low_price numeric(20,6),
    change numeric(20,6),
    change_pct numeric(12,6),
    amount numeric(24,6),
    total_hand numeric(24,6),
    current_hand numeric(24,6),
    source text NOT NULL DEFAULT 'eltdx',
    fetched_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdx_industry_quote_snapshot_date_code_source_alive
    ON cynexus_appl_market.tdx_industry_quote_snapshot (snapshot_date, code6, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tdx_industry_quote_snapshot_alive
    ON cynexus_appl_market.tdx_industry_quote_snapshot (snapshot_date DESC, change_pct DESC)
    WHERE deleted_at IS NULL;
