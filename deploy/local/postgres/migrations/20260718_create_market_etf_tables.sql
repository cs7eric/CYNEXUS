CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_catalog
(
    id bigint PRIMARY KEY,
    code varchar(6) NOT NULL,
    exchange varchar(4) NOT NULL,
    name text NOT NULL,
    category varchar(16) NOT NULL,
    theme text,
    source text NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_etf_catalog_category CHECK (category IN ('broad', 'sector', 'other'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_etf_catalog_code_alive
    ON cynexus_appl_market.market_etf_catalog (code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_catalog_category_active
    ON cynexus_appl_market.market_etf_catalog (category, is_active, name)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_spot_snapshot
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    snapshot_minute timestamptz NOT NULL,
    captured_at timestamptz NOT NULL,
    code varchar(6) NOT NULL,
    exchange varchar(4) NOT NULL,
    name text NOT NULL,
    category varchar(16) NOT NULL,
    theme text,
    price numeric(20, 6),
    prev_close numeric(20, 6),
    change_pct numeric(12, 6),
    volume numeric(24, 4),
    amount numeric(24, 4),
    iopv numeric(20, 6),
    premium_pct numeric(12, 6),
    turnover_pct numeric(12, 6),
    inside_volume numeric(24, 4),
    outside_volume numeric(24, 4),
    latest_shares numeric(24, 4),
    main_net_inflow numeric(24, 4),
    main_net_ratio numeric(12, 6),
    large_net_inflow numeric(24, 4),
    large_net_ratio numeric(12, 6),
    source text NOT NULL,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_etf_spot_category CHECK (category IN ('broad', 'sector', 'other'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_etf_spot_snapshot_key_alive
    ON cynexus_appl_market.market_etf_spot_snapshot (trade_date, snapshot_minute, code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_spot_latest
    ON cynexus_appl_market.market_etf_spot_snapshot (trade_date DESC, snapshot_minute DESC, category, code)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_daily
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(6) NOT NULL,
    exchange varchar(4) NOT NULL,
    name text NOT NULL,
    category varchar(16) NOT NULL,
    theme text,
    close_price numeric(20, 6),
    iopv numeric(20, 6),
    premium_pct numeric(12, 6),
    volume numeric(24, 4),
    amount numeric(24, 4),
    total_shares numeric(24, 4),
    previous_shares numeric(24, 4),
    share_delta numeric(24, 4),
    net_subscription_proxy numeric(24, 4),
    turnover_pct numeric(12, 6),
    source text NOT NULL,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_etf_daily_category CHECK (category IN ('broad', 'sector', 'other'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_etf_daily_date_code_source_alive
    ON cynexus_appl_market.market_etf_daily (trade_date, code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_daily_category_date
    ON cynexus_appl_market.market_etf_daily (category, trade_date DESC, amount DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_daily_code_date
    ON cynexus_appl_market.market_etf_daily (code, trade_date DESC)
    WHERE deleted_at IS NULL;

ALTER TABLE cynexus_appl_market.market_etf_spot_snapshot
    ADD COLUMN IF NOT EXISTS super_large_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS super_large_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS medium_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS medium_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS small_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS small_net_ratio numeric(12, 6);

ALTER TABLE cynexus_appl_market.market_etf_daily
    ADD COLUMN IF NOT EXISTS cumulative_share_delta numeric(24, 4),
    ADD COLUMN IF NOT EXISTS cumulative_net_subscription_proxy numeric(24, 4),
    ADD COLUMN IF NOT EXISTS inside_volume numeric(24, 4),
    ADD COLUMN IF NOT EXISTS outside_volume numeric(24, 4),
    ADD COLUMN IF NOT EXISTS super_large_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS super_large_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS main_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS main_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS large_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS large_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS medium_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS medium_net_ratio numeric(12, 6),
    ADD COLUMN IF NOT EXISTS small_net_inflow numeric(24, 4),
    ADD COLUMN IF NOT EXISTS small_net_ratio numeric(12, 6);
