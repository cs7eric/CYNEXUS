CREATE TABLE IF NOT EXISTS cynexus_appl_market.tdx_industry_fund_flow_batch
(
    id bigint PRIMARY KEY,
    batch_key text NOT NULL,
    trade_date date NOT NULL,
    source text NOT NULL DEFAULT 'eltdx.f10.theme_market.200742',
    req_id text NOT NULL DEFAULT '200742',
    row_count integer NOT NULL DEFAULT 0,
    ok_count integer NOT NULL DEFAULT 0,
    empty_count integer NOT NULL DEFAULT 0,
    error_count integer NOT NULL DEFAULT 0,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdx_industry_fund_flow_batch_key_alive
    ON cynexus_appl_market.tdx_industry_fund_flow_batch (batch_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tdx_industry_fund_flow_batch_trade_date
    ON cynexus_appl_market.tdx_industry_fund_flow_batch (trade_date DESC, source)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.tdx_industry_fund_flow_snapshot
(
    id bigint PRIMARY KEY,
    batch_id bigint NOT NULL REFERENCES cynexus_appl_market.tdx_industry_fund_flow_batch(id),
    trade_date date NOT NULL,
    code6 varchar(6) NOT NULL,
    full_code varchar(8) NOT NULL,
    tdx_name text NOT NULL,
    flow_sector_name text,
    seed_code varchar(8),
    flow_trade_date date,
    snapshot_status text NOT NULL,
    selection_mode text NOT NULL,
    name_match_type text NOT NULL,
    main_net numeric(20, 2),
    large_order_net numeric(20, 2),
    medium_order_net numeric(20, 2),
    small_order_net numeric(20, 2),
    row_count integer NOT NULL DEFAULT 0,
    source text NOT NULL DEFAULT 'eltdx.f10.theme_market.200742',
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    generated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_tdx_industry_fund_flow_snapshot_status
        CHECK (snapshot_status IN ('ok', 'empty', 'error')),
    CONSTRAINT ck_tdx_industry_fund_flow_selection_mode
        CHECK (selection_mode IN ('exact', 'fallback_previous', 'latest_any', 'empty')),
    CONSTRAINT ck_tdx_industry_fund_flow_name_match_type
        CHECK (name_match_type IN ('exact', 'mismatch', 'empty'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tdx_industry_fund_flow_snapshot_date_code_source_alive
    ON cynexus_appl_market.tdx_industry_fund_flow_snapshot (trade_date, code6, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tdx_industry_fund_flow_snapshot_code_trade_date
    ON cynexus_appl_market.tdx_industry_fund_flow_snapshot (code6, trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tdx_industry_fund_flow_snapshot_trade_date
    ON cynexus_appl_market.tdx_industry_fund_flow_snapshot (trade_date DESC, snapshot_status, name_match_type)
    WHERE deleted_at IS NULL;
