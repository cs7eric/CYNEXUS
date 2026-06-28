CREATE SCHEMA IF NOT EXISTS cynexus_data;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cynexus_data.sector_fund_flow_capture_batch
(
    batch_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    scope text NOT NULL DEFAULT 'industry',
    trade_date date NOT NULL,
    source_type text NOT NULL DEFAULT 'crawler',
    status text NOT NULL DEFAULT 'success',
    source text NOT NULL DEFAULT 'ths.10jqka.com.cn',
    total_pages integer,
    row_count integer NOT NULL DEFAULT 0,
    page_row_counts jsonb NOT NULL DEFAULT '[]'::jsonb,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_sector_fund_flow_capture_batch_scope CHECK (scope IN ('industry', 'concept')),
    CONSTRAINT ck_sector_fund_flow_capture_batch_source_type CHECK (source_type IN ('crawler', 'json_import')),
    CONSTRAINT ck_sector_fund_flow_capture_batch_status CHECK (status IN ('success', 'partial', 'failed'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sector_fund_flow_capture_batch_scope_trade_date_alive
    ON cynexus_data.sector_fund_flow_capture_batch (scope, trade_date)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sector_fund_flow_capture_batch_scope_fetched_at
    ON cynexus_data.sector_fund_flow_capture_batch (scope, fetched_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_data.sector_fund_flow_daily_snapshot
(
    snapshot_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id uuid NOT NULL REFERENCES cynexus_data.sector_fund_flow_capture_batch(batch_id) ON DELETE CASCADE,
    scope text NOT NULL DEFAULT 'industry',
    trade_date date NOT NULL,
    sector_code text,
    sector_name text NOT NULL,
    rank integer,
    change_pct numeric(12,6),
    inflow numeric(24,6),
    outflow numeric(24,6),
    net numeric(24,6),
    company_count integer,
    leader_stock text,
    leader_change numeric(12,6),
    leader_price numeric(24,6),
    source text NOT NULL DEFAULT 'ths.10jqka.com.cn',
    source_type text NOT NULL DEFAULT 'crawler',
    captured_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_sector_fund_flow_daily_snapshot_scope CHECK (scope IN ('industry', 'concept')),
    CONSTRAINT ck_sector_fund_flow_daily_snapshot_source_type CHECK (source_type IN ('crawler', 'json_import'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sector_fund_flow_daily_snapshot_scope_trade_date_sector_alive
    ON cynexus_data.sector_fund_flow_daily_snapshot (scope, trade_date, sector_name)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sector_fund_flow_daily_snapshot_scope_trade_date_net
    ON cynexus_data.sector_fund_flow_daily_snapshot (scope, trade_date, net DESC, rank ASC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sector_fund_flow_daily_snapshot_scope_sector_trade_date
    ON cynexus_data.sector_fund_flow_daily_snapshot (scope, sector_name, trade_date DESC)
    WHERE deleted_at IS NULL;
