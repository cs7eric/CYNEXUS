\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

-- MSI result tables migrated from DuckDB. Primary keys remain project-wide
-- BIGINT IDs assigned by the application/migration Snowflake layer; DuckDB
-- natural keys are enforced with alive-only unique indexes.

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_index_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    vol_score numeric(6, 2),
    turnover_score numeric(6, 2),
    price_strength_score numeric(6, 2),
    risk_appetite_score numeric(6, 2),
    breadth_score numeric(6, 2),
    limit_emotion_score numeric(6, 2),
    profit_effect_score numeric(6, 2),
    sector_breadth_score numeric(6, 2),
    style_risk_score numeric(6, 2),
    composite_score numeric(6, 2) NOT NULL,
    component_count integer NOT NULL,
    level varchar(64) NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_index_daily_trade_date_alive
    ON cynexus_appl_market.msi_index_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_index_daily_trade_date_desc
    ON cynexus_appl_market.msi_index_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_limit_emotion_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    limit_up_count integer NOT NULL,
    limit_down_count integer NOT NULL,
    touched_count integer NOT NULL,
    broken_count integer NOT NULL,
    break_board_rate numeric(6, 4) NOT NULL,
    limit_up_down_ratio numeric(10, 4),
    yesterday_limit_up_count integer NOT NULL,
    yesterday_limit_up_avg_return numeric(8, 4) NOT NULL,
    up_down_score numeric(6, 2) NOT NULL,
    break_board_score numeric(6, 2) NOT NULL,
    yesterday_return_score numeric(6, 2) NOT NULL,
    composite_score numeric(6, 2) NOT NULL,
    level varchar(64) NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_limit_emotion_daily_trade_date_alive
    ON cynexus_appl_market.msi_limit_emotion_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_limit_emotion_daily_trade_date_desc
    ON cynexus_appl_market.msi_limit_emotion_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_ma_count_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    total_eligible integer NOT NULL,
    above_ma20 integer NOT NULL,
    above_ma60 integer NOT NULL,
    above_both integer NOT NULL,
    pct_ma20 numeric(6, 2) NOT NULL,
    pct_ma60 numeric(6, 2) NOT NULL,
    pct_both numeric(6, 2) NOT NULL,
    by_board jsonb NOT NULL DEFAULT '{}'::jsonb,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    up_5d_count integer,
    up_5d_pct numeric(6, 2),
    new_low_60d_count integer,
    new_low_60d_pct numeric(6, 2),
    new_high_252d_count integer,
    new_high_252d_pct numeric(6, 2),
    advancing_count integer,
    advancing_pct numeric(6, 2),
    breadth_raw numeric(6, 2),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_ma_count_daily_trade_date_alive
    ON cynexus_appl_market.msi_ma_count_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_ma_count_daily_trade_date_desc
    ON cynexus_appl_market.msi_ma_count_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_profit_effect_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    up_5d_pct numeric(6, 2) NOT NULL,
    new_low_60d_pct numeric(6, 2) NOT NULL,
    score numeric(6, 2) NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_profit_effect_daily_trade_date_alive
    ON cynexus_appl_market.msi_profit_effect_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_profit_effect_daily_trade_date_desc
    ON cynexus_appl_market.msi_profit_effect_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_risk_appetite_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    hs300_close numeric(12, 4) NOT NULL,
    hs300_base_close numeric(12, 4) NOT NULL,
    hs300_base_date date NOT NULL,
    hs300_20d_return numeric(8, 4) NOT NULL,
    treasury_511010_close numeric(12, 4),
    treasury_511010_base_close numeric(12, 4),
    treasury_511010_20d_return numeric(8, 4),
    treasury_511090_close numeric(12, 4),
    treasury_511090_base_close numeric(12, 4),
    treasury_511090_20d_return numeric(8, 4),
    treasury_weighted_20d_return numeric(8, 4) NOT NULL,
    spread_511010 numeric(8, 4) NOT NULL,
    spread_511090 numeric(8, 4) NOT NULL,
    spread_weighted numeric(8, 4) NOT NULL,
    window_days integer NOT NULL DEFAULT 20,
    source varchar(128) NOT NULL,
    elapsed_ms integer,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_risk_appetite_daily_trade_date_alive
    ON cynexus_appl_market.msi_risk_appetite_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_risk_appetite_daily_trade_date_desc
    ON cynexus_appl_market.msi_risk_appetite_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_style_risk_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    window_days integer NOT NULL DEFAULT 5,
    hs300_return numeric(8, 4) NOT NULL,
    csi1000_return numeric(8, 4) NOT NULL,
    spread numeric(8, 4) NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_style_risk_daily_trade_date_alive
    ON cynexus_appl_market.msi_style_risk_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_style_risk_daily_trade_date_desc
    ON cynexus_appl_market.msi_style_risk_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_turnover_activity_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    total_amount numeric(18, 4) NOT NULL,
    avg_20d_amount numeric(18, 4) NOT NULL,
    ratio numeric(8, 4) NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    score numeric(6, 2),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_turnover_activity_daily_trade_date_alive
    ON cynexus_appl_market.msi_turnover_activity_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_turnover_activity_daily_trade_date_desc
    ON cynexus_appl_market.msi_turnover_activity_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_volatility_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    underlying_code varchar(32) NOT NULL,
    underlying_name varchar(128) NOT NULL,
    close numeric(12, 4) NOT NULL,
    daily_return_pct numeric(8, 4),
    realized_vol_20d numeric(6, 2) NOT NULL,
    vol_window_days integer NOT NULL DEFAULT 20,
    vol_lookback_days integer NOT NULL DEFAULT 252,
    percentile_1y numeric(6, 4) NOT NULL,
    sentiment_score numeric(6, 2) NOT NULL,
    sample_count integer NOT NULL,
    elapsed_ms integer,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_volatility_daily_trade_date_alive
    ON cynexus_appl_market.msi_volatility_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_volatility_daily_trade_date_desc
    ON cynexus_appl_market.msi_volatility_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.msi_sector_breadth_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    advancing integer NOT NULL,
    declining integer NOT NULL,
    flat integer NOT NULL,
    total integer NOT NULL,
    advance_pct numeric(6, 4) NOT NULL,
    source varchar(128) NOT NULL,
    elapsed_ms integer,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msi_sector_breadth_daily_trade_date_alive
    ON cynexus_appl_market.msi_sector_breadth_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_msi_sector_breadth_daily_trade_date_desc
    ON cynexus_appl_market.msi_sector_breadth_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

-- General market result tables.

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_overview_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    total_amount numeric(18, 4),
    total_volume numeric(18, 4),
    rising_count integer,
    falling_count integer,
    flat_count integer,
    limit_up_count integer,
    limit_down_count integer,
    stock_count integer,
    main_net_inflow numeric(18, 4),
    super_large_net_inflow numeric(18, 4),
    large_net_inflow numeric(18, 4),
    medium_net_inflow numeric(18, 4),
    small_net_inflow numeric(18, 4),
    main_net_inflow_ratio numeric(6, 2),
    super_large_net_ratio numeric(6, 2),
    large_net_ratio numeric(6, 2),
    medium_net_ratio numeric(6, 2),
    small_net_ratio numeric(6, 2),
    source varchar(128),
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_overview_daily_trade_date_alive
    ON cynexus_appl_market.mkt_overview_daily (trade_date)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_overview_daily_trade_date_desc
    ON cynexus_appl_market.mkt_overview_daily (trade_date DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_sector_pulse_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    sector_name varchar(128) NOT NULL,
    sector_index varchar(64),
    change_pct numeric(8, 4) NOT NULL,
    inflow numeric(18, 4) NOT NULL DEFAULT 0,
    outflow numeric(18, 4) NOT NULL DEFAULT 0,
    main_net numeric(18, 4) NOT NULL DEFAULT 0,
    stock_count integer,
    leading_stock varchar(128),
    leading_change_pct numeric(8, 4),
    leading_price numeric(12, 4),
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_sector_pulse_daily_trade_date_sector_alive
    ON cynexus_appl_market.mkt_sector_pulse_daily (trade_date, sector_name)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_sector_pulse_daily_trade_date_change
    ON cynexus_appl_market.mkt_sector_pulse_daily (trade_date, change_pct DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_sector_pulse_daily_trade_date_main_net
    ON cynexus_appl_market.mkt_sector_pulse_daily (trade_date, main_net DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_ths_industry_fund_flow_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    industry varchar(128) NOT NULL,
    industry_code varchar(64),
    rank integer,
    change_pct numeric(8, 4) NOT NULL,
    inflow numeric(18, 4) NOT NULL DEFAULT 0,
    outflow numeric(18, 4) NOT NULL DEFAULT 0,
    net numeric(18, 4) NOT NULL DEFAULT 0,
    company_count integer,
    leader_stock varchar(128),
    leader_change numeric(8, 4),
    leader_price numeric(12, 4),
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_ths_industry_fund_flow_daily_trade_date_industry_alive
    ON cynexus_appl_market.mkt_ths_industry_fund_flow_daily (trade_date, industry)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_ths_industry_fund_flow_daily_industry_code
    ON cynexus_appl_market.mkt_ths_industry_fund_flow_daily (industry_code)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_ths_industry_fund_flow_daily_trade_date_net
    ON cynexus_appl_market.mkt_ths_industry_fund_flow_daily (trade_date, net DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_index_return_daily (
    id bigint NOT NULL PRIMARY KEY,
    trade_date date NOT NULL,
    index_code varchar(32) NOT NULL,
    index_name varchar(128) NOT NULL,
    window_days integer NOT NULL,
    current_close numeric(12, 4) NOT NULL,
    current_trade_date date NOT NULL,
    base_close numeric(12, 4) NOT NULL,
    base_date date NOT NULL,
    return_pct numeric(8, 4) NOT NULL,
    source varchar(128) NOT NULL,
    ingested_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_index_return_daily_trade_date_code_window_alive
    ON cynexus_appl_market.mkt_index_return_daily (trade_date, index_code, window_days)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_index_return_daily_trade_date_desc
    ON cynexus_appl_market.mkt_index_return_daily (trade_date DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_mkt_index_return_daily_index_code_date
    ON cynexus_appl_market.mkt_index_return_daily (index_code, trade_date DESC)
    WHERE deleted_at IS NULL;

-- Security master data.

CREATE TABLE IF NOT EXISTS cynexus_appl_market.sec_stock_universe (
    id bigint NOT NULL PRIMARY KEY,
    code varchar(32) NOT NULL,
    name varchar(128) NOT NULL,
    market varchar(32) NOT NULL,
    sector varchar(128),
    ipo_date date,
    delist_date date,
    is_active boolean NOT NULL DEFAULT true,
    last_seen date,
    source_updated_at timestamptz,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sec_stock_universe_code_alive
    ON cynexus_appl_market.sec_stock_universe (code)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sec_stock_universe_market
    ON cynexus_appl_market.sec_stock_universe (market)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sec_stock_universe_active
    ON cynexus_appl_market.sec_stock_universe (is_active)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.sec_corp_event (
    id bigint NOT NULL PRIMARY KEY,
    code varchar(32) NOT NULL,
    ex_date date NOT NULL,
    event_type varchar(64) NOT NULL,
    ratio numeric(10, 6) NOT NULL,
    factor numeric(12, 8) NOT NULL,
    source varchar(128) NOT NULL,
    fetched_at timestamptz NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sec_corp_event_code_ex_date_type_ratio_alive
    ON cynexus_appl_market.sec_corp_event (code, ex_date, event_type, ratio)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sec_corp_event_code_ex_date
    ON cynexus_appl_market.sec_corp_event (code, ex_date DESC)
    WHERE deleted_at IS NULL;

COMMIT;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'cynexus_appl_market'
  AND table_name IN (
      'msi_index_daily',
      'msi_limit_emotion_daily',
      'msi_ma_count_daily',
      'msi_profit_effect_daily',
      'msi_risk_appetite_daily',
      'msi_style_risk_daily',
      'msi_turnover_activity_daily',
      'msi_volatility_daily',
      'msi_sector_breadth_daily',
      'mkt_overview_daily',
      'mkt_sector_pulse_daily',
      'mkt_ths_industry_fund_flow_daily',
      'mkt_index_return_daily',
      'sec_stock_universe',
      'sec_corp_event'
  )
ORDER BY table_name;
