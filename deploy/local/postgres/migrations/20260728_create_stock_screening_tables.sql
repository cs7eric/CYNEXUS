CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

ALTER TABLE cynexus_appl_market.sec_stock_basic
    ADD COLUMN IF NOT EXISTS company_name_en text,
    ADD COLUMN IF NOT EXISTS listing_status text NOT NULL DEFAULT 'listed',
    ADD COLUMN IF NOT EXISTS delisting_date date,
    ADD COLUMN IF NOT EXISTS province text,
    ADD COLUMN IF NOT EXISTS city text,
    ADD COLUMN IF NOT EXISTS main_business text,
    ADD COLUMN IF NOT EXISTS website text,
    ADD COLUMN IF NOT EXISTS legal_representative text,
    ADD COLUMN IF NOT EXISTS registered_capital numeric(24, 4),
    ADD COLUMN IF NOT EXISTS profile_updated_at timestamptz;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_financial_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code varchar(6) NOT NULL,
    report_type text NOT NULL,
    report_date date NOT NULL,
    announced_date date,
    revenue numeric(24, 6),
    revenue_yoy_pct numeric(18, 6),
    net_profit numeric(24, 6),
    net_profit_yoy_pct numeric(18, 6),
    net_profit_attributable numeric(24, 6),
    deducted_net_profit numeric(24, 6),
    operating_cash_flow numeric(24, 6),
    total_assets numeric(24, 6),
    total_liabilities numeric(24, 6),
    total_equity numeric(24, 6),
    eps numeric(18, 6),
    book_value_per_share numeric(18, 6),
    roe_pct numeric(18, 6),
    gross_margin_pct numeric(18, 6),
    net_margin_pct numeric(18, 6),
    asset_liability_ratio_pct numeric(18, 6),
    operating_cash_flow_per_share numeric(18, 6),
    pe numeric(18, 6),
    pb numeric(18, 6),
    dividend_yield_pct numeric(18, 6),
    fields jsonb NOT NULL DEFAULT '{}'::jsonb,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_financial_snapshot
    ON cynexus_appl_market.a_stock_financial_snapshot (source, code, report_type, report_date);
CREATE INDEX IF NOT EXISTS idx_a_stock_financial_snapshot_screening
    ON cynexus_appl_market.a_stock_financial_snapshot (code, report_date DESC, report_type);
CREATE INDEX IF NOT EXISTS idx_a_stock_financial_snapshot_quality
    ON cynexus_appl_market.a_stock_financial_snapshot (report_date DESC, roe_pct, revenue_yoy_pct);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_daily_screening_fact (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code varchar(6) NOT NULL,
    trade_date date NOT NULL,
    open_price numeric(20, 6),
    high_price numeric(20, 6),
    low_price numeric(20, 6),
    close_price numeric(20, 6),
    prev_close numeric(20, 6),
    change_pct numeric(18, 6),
    amplitude_pct numeric(18, 6),
    volume numeric(24, 6),
    amount numeric(24, 6),
    turnover_rate_pct numeric(18, 6),
    total_market_cap numeric(24, 6),
    float_market_cap numeric(24, 6),
    adjusted_close numeric(20, 6),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_daily_screening_fact
    ON cynexus_appl_market.a_stock_daily_screening_fact (source, code, trade_date);
CREATE INDEX IF NOT EXISTS idx_a_stock_daily_screening_fact_code_date
    ON cynexus_appl_market.a_stock_daily_screening_fact (code, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_a_stock_daily_screening_fact_liquidity
    ON cynexus_appl_market.a_stock_daily_screening_fact (trade_date DESC, amount DESC, turnover_rate_pct DESC);
