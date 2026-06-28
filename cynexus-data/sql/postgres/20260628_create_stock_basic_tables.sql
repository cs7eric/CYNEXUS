CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.sec_stock_basic
(
    code text PRIMARY KEY,
    name text NOT NULL DEFAULT '',
    market text NOT NULL DEFAULT '',
    board text NOT NULL DEFAULT 'unknown',
    is_st boolean NOT NULL DEFAULT false,
    limit_up_threshold numeric(8,4) NOT NULL DEFAULT 0.10,
    is_active boolean NOT NULL DEFAULT true,
    source text NOT NULL DEFAULT 'akshare',
    fetched_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE cynexus_data.sec_stock_basic
    ADD COLUMN IF NOT EXISTS industry text,
    ADD COLUMN IF NOT EXISTS total_market_cap numeric(24,4),
    ADD COLUMN IF NOT EXISTS float_market_cap numeric(24,4),
    ADD COLUMN IF NOT EXISTS cap_style text,
    ADD COLUMN IF NOT EXISTS sector_index_symbol text,
    ADD COLUMN IF NOT EXISTS sector_index_name text,
    ADD COLUMN IF NOT EXISTS industry_query text;

CREATE INDEX IF NOT EXISTS idx_sec_stock_basic_active_code
    ON cynexus_data.sec_stock_basic (is_active, code);

CREATE INDEX IF NOT EXISTS idx_sec_stock_basic_industry
    ON cynexus_data.sec_stock_basic (industry, code)
    WHERE industry IS NOT NULL;

CREATE TABLE IF NOT EXISTS cynexus_data.sec_trade_calendar
(
    trade_date date PRIMARY KEY,
    is_trading_day boolean NOT NULL DEFAULT false,
    reason text NOT NULL DEFAULT 'weekday',
    source text NOT NULL DEFAULT 'local_rules',
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sec_trade_calendar_trading_day
    ON cynexus_data.sec_trade_calendar (is_trading_day, trade_date);
