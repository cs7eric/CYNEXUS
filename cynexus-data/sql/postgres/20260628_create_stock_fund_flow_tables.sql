CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;
CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_fund_flow_capture_batch
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    source_type text NOT NULL DEFAULT 'crawler',
    status text NOT NULL DEFAULT 'success',
    source text NOT NULL DEFAULT 'tencent.qt',
    row_count integer NOT NULL DEFAULT 0,
    requested_codes jsonb NOT NULL DEFAULT '[]'::jsonb,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_stock_fund_flow_capture_batch_source_type CHECK (source_type IN ('crawler', 'json_import')),
    CONSTRAINT ck_stock_fund_flow_capture_batch_status CHECK (status IN ('success', 'partial', 'failed'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_fund_flow_capture_batch_trade_date_source_alive
    ON cynexus_appl_market.stock_fund_flow_capture_batch (trade_date, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_stock_fund_flow_capture_batch_fetched_at
    ON cynexus_appl_market.stock_fund_flow_capture_batch (fetched_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_fund_flow_snapshot
(
    id bigint PRIMARY KEY,
    batch_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_fund_flow_capture_batch(id) ON DELETE CASCADE,
    trade_date date NOT NULL,
    code text NOT NULL,
    symbol text,
    name text,
    last_price numeric(24,6),
    pre_close numeric(24,6),
    open numeric(24,6),
    high numeric(24,6),
    low numeric(24,6),
    change numeric(24,6),
    change_pct numeric(12,6),
    volume_lots numeric(24,6),
    amount_wan numeric(24,6),
    turnover_rate numeric(12,6),
    outer_disc_lots numeric(24,6),
    inside_dish_lots numeric(24,6),
    active_net_lots numeric(24,6),
    active_net_amount_wan numeric(24,6),
    active_buy_ratio numeric(12,8),
    active_sell_ratio numeric(12,8),
    buy_big_ratio numeric(12,8),
    buy_small_ratio numeric(12,8),
    sell_big_ratio numeric(12,8),
    sell_small_ratio numeric(12,8),
    pe numeric(24,6),
    pb numeric(24,6),
    amplitude numeric(12,6),
    float_market_cap_yi numeric(24,6),
    total_market_cap_yi numeric(24,6),
    source text NOT NULL DEFAULT 'tencent.qt',
    source_type text NOT NULL DEFAULT 'crawler',
    captured_at timestamptz NOT NULL DEFAULT now(),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_stock_fund_flow_snapshot_source_type CHECK (source_type IN ('crawler', 'json_import'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_fund_flow_snapshot_trade_date_code_source_alive
    ON cynexus_appl_market.stock_fund_flow_snapshot (trade_date, code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_stock_fund_flow_snapshot_trade_date_net
    ON cynexus_appl_market.stock_fund_flow_snapshot (trade_date, active_net_amount_wan DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_stock_fund_flow_snapshot_code_trade_date
    ON cynexus_appl_market.stock_fund_flow_snapshot (code, trade_date DESC)
    WHERE deleted_at IS NULL;
