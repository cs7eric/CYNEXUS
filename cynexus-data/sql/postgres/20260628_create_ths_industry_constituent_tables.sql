CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.ths_industry_constituent_capture_batch
(
    id bigint PRIMARY KEY,
    snapshot_date date NOT NULL,
    source_type text NOT NULL,
    status text NOT NULL DEFAULT 'success',
    source text NOT NULL DEFAULT 'q.10jqka.com.cn',
    requested_industry_codes jsonb NOT NULL DEFAULT '[]'::jsonb,
    industry_count integer NOT NULL DEFAULT 0,
    row_count integer NOT NULL DEFAULT 0,
    page_row_counts jsonb NOT NULL DEFAULT '{}'::jsonb,
    fetched_at timestamptz,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_ths_industry_constituent_batch_date_source
    ON cynexus_appl_market.ths_industry_constituent_capture_batch (snapshot_date, source)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.ths_industry_constituent_snapshot
(
    id bigint PRIMARY KEY,
    batch_id bigint NOT NULL REFERENCES cynexus_appl_market.ths_industry_constituent_capture_batch(id) ON DELETE CASCADE,
    snapshot_date date NOT NULL,
    industry_code text NOT NULL,
    stock_code text NOT NULL,
    stock_name text,
    rank integer,
    price numeric(20,6),
    change_pct numeric(20,6),
    change numeric(20,6),
    speed_pct numeric(20,6),
    turnover_pct numeric(20,6),
    volume_ratio numeric(20,6),
    amplitude_pct numeric(20,6),
    amount_text text,
    float_shares_text text,
    float_market_cap_text text,
    pe numeric(20,6),
    source text NOT NULL DEFAULT 'q.10jqka.com.cn',
    source_type text NOT NULL DEFAULT 'crawler',
    captured_at timestamptz,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_ths_industry_constituent_snapshot_industry
    ON cynexus_appl_market.ths_industry_constituent_snapshot (snapshot_date, industry_code, rank)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ths_industry_constituent_snapshot_stock
    ON cynexus_appl_market.ths_industry_constituent_snapshot (stock_code, snapshot_date)
    WHERE deleted_at IS NULL;
