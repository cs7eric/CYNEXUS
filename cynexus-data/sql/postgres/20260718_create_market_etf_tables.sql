CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_catalog
(
    id bigint PRIMARY KEY,
    code varchar(6) NOT NULL,
    exchange varchar(4) NOT NULL,
    name text NOT NULL,
    category varchar(16) NOT NULL,
    theme text,
    classification_source text NOT NULL DEFAULT 'fallback.name_keyword',
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
    classification_source text NOT NULL DEFAULT 'fallback.name_keyword',
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
    super_large_net_inflow numeric(24, 4),
    super_large_net_ratio numeric(12, 6),
    main_net_inflow numeric(24, 4),
    main_net_ratio numeric(12, 6),
    large_net_inflow numeric(24, 4),
    large_net_ratio numeric(12, 6),
    medium_net_inflow numeric(24, 4),
    medium_net_ratio numeric(12, 6),
    small_net_inflow numeric(24, 4),
    small_net_ratio numeric(12, 6),
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
    classification_source text NOT NULL DEFAULT 'fallback.name_keyword',
    close_price numeric(20, 6),
    iopv numeric(20, 6),
    premium_pct numeric(12, 6),
    volume numeric(24, 4),
    amount numeric(24, 4),
    total_shares numeric(24, 4),
    previous_shares numeric(24, 4),
    share_delta numeric(24, 4),
    net_subscription_proxy numeric(24, 4),
    cumulative_share_delta numeric(24, 4),
    cumulative_net_subscription_proxy numeric(24, 4),
    turnover_pct numeric(12, 6),
    inside_volume numeric(24, 4),
    outside_volume numeric(24, 4),
    super_large_net_inflow numeric(24, 4),
    super_large_net_ratio numeric(12, 6),
    main_net_inflow numeric(24, 4),
    main_net_ratio numeric(12, 6),
    large_net_inflow numeric(24, 4),
    large_net_ratio numeric(12, 6),
    medium_net_inflow numeric(24, 4),
    medium_net_ratio numeric(12, 6),
    small_net_inflow numeric(24, 4),
    small_net_ratio numeric(12, 6),
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

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_mapping
(
    id bigint PRIMARY KEY,
    etf_code varchar(6) NOT NULL,
    mapping_group varchar(32) NOT NULL,
    mapping_code varchar(64) NOT NULL,
    mapping_name text NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    confidence numeric(5, 4),
    source text NOT NULL,
    effective_from date NOT NULL DEFAULT CURRENT_DATE,
    effective_to date,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_etf_mapping_group CHECK (mapping_group IN ('universe', 'asset_class', 'region', 'index_scope', 'style', 'theme')),
    CONSTRAINT ck_market_etf_mapping_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_etf_mapping_key_alive
    ON cynexus_appl_market.market_etf_mapping (etf_code, mapping_group, mapping_code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_mapping_group_code
    ON cynexus_appl_market.market_etf_mapping (mapping_group, mapping_code, etf_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_mapping_etf_code
    ON cynexus_appl_market.market_etf_mapping (etf_code, mapping_group, is_primary)
    WHERE deleted_at IS NULL;
