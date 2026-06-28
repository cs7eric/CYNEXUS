CREATE SCHEMA IF NOT EXISTS cynexus_data;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cynexus_data.market_pulse_capture_batch
(
    batch_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_date date NOT NULL,
    source_kind text NOT NULL DEFAULT 'fund_flow_snapshot',
    status text NOT NULL DEFAULT 'success',
    source_name text NOT NULL DEFAULT 'sector_fund_flow_daily_snapshot',
    row_count integer NOT NULL DEFAULT 0,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_pulse_capture_batch_source_kind CHECK (source_kind IN ('fund_flow_snapshot', 'live_capture', 'duckdb_import', 'json_import')),
    CONSTRAINT ck_market_pulse_capture_batch_status CHECK (status IN ('success', 'partial', 'failed'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_pulse_capture_batch_trade_date_alive
    ON cynexus_data.market_pulse_capture_batch (trade_date)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_data.market_pulse_sector_daily_snapshot
(
    snapshot_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id uuid NOT NULL REFERENCES cynexus_data.market_pulse_capture_batch(batch_id) ON DELETE CASCADE,
    trade_date date NOT NULL,
    sector_name text NOT NULL,
    sector_index text,
    rank_by_change integer,
    change_pct numeric(12,6),
    inflow numeric(24,6),
    outflow numeric(24,6),
    main_net numeric(24,6),
    stock_count integer,
    leading_stock text,
    leading_change_pct numeric(12,6),
    leading_price numeric(24,6),
    source_kind text NOT NULL DEFAULT 'fund_flow_snapshot',
    source_name text NOT NULL DEFAULT 'sector_fund_flow_daily_snapshot',
    captured_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_pulse_sector_daily_snapshot_trade_date_sector_alive
    ON cynexus_data.market_pulse_sector_daily_snapshot (trade_date, sector_name)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_pulse_sector_daily_snapshot_trade_date_rank
    ON cynexus_data.market_pulse_sector_daily_snapshot (trade_date, rank_by_change ASC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_pulse_sector_daily_snapshot_trade_date_main_net
    ON cynexus_data.market_pulse_sector_daily_snapshot (trade_date, main_net DESC)
    WHERE deleted_at IS NULL;
