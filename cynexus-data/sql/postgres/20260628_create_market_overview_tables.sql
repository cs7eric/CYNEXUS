CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.market_overview_snapshot
(
    trade_date date PRIMARY KEY,
    total_amount numeric(18,4),
    total_volume numeric(18,4),
    rising_count integer,
    falling_count integer,
    flat_count integer,
    limit_up_count integer,
    limit_down_count integer,
    stock_count integer,
    main_net_inflow numeric(18,4),
    super_large_net_inflow numeric(18,4),
    large_net_inflow numeric(18,4),
    medium_net_inflow numeric(18,4),
    small_net_inflow numeric(18,4),
    main_net_inflow_ratio numeric(8,4),
    super_large_net_ratio numeric(8,4),
    large_net_ratio numeric(8,4),
    medium_net_ratio numeric(8,4),
    small_net_ratio numeric(8,4),
    source text,
    is_manual_override boolean NOT NULL DEFAULT false,
    manual_updated_at timestamptz,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_market_overview_snapshot_trade_date_alive
    ON cynexus_data.market_overview_snapshot (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_data.market_overview_detail_snapshot
(
    trade_date date NOT NULL,
    benchmark_code text NOT NULL,
    source_table text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    source text NOT NULL DEFAULT 'python',
    generated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (trade_date, benchmark_code, source_table)
);

CREATE INDEX IF NOT EXISTS idx_market_overview_detail_snapshot_alive
    ON cynexus_data.market_overview_detail_snapshot (trade_date DESC, benchmark_code)
    WHERE deleted_at IS NULL;
