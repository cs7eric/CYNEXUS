CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.margin_market_summary
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    exchange text NOT NULL,
    source text NOT NULL,
    financing_balance numeric(20,4),
    financing_buy_amount numeric(20,4),
    financing_repay_amount numeric(20,4),
    securities_lending_balance numeric(20,4),
    securities_lending_balance_amount numeric(20,4),
    securities_lending_sell_volume numeric(20,4),
    securities_lending_repay_volume numeric(20,4),
    margin_balance numeric(20,4),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_margin_market_summary_active
    ON cynexus_appl_market.margin_market_summary (trade_date, exchange, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_margin_market_summary_date
    ON cynexus_appl_market.margin_market_summary (trade_date)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.margin_stock_detail
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    exchange text NOT NULL,
    stock_code text NOT NULL,
    stock_name text,
    source text NOT NULL,
    financing_balance numeric(20,4),
    financing_buy_amount numeric(20,4),
    financing_repay_amount numeric(20,4),
    securities_lending_balance numeric(20,4),
    securities_lending_balance_amount numeric(20,4),
    securities_lending_sell_volume numeric(20,4),
    securities_lending_repay_volume numeric(20,4),
    margin_balance numeric(20,4),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_margin_stock_detail_active
    ON cynexus_appl_market.margin_stock_detail (trade_date, exchange, stock_code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_margin_stock_detail_code_date
    ON cynexus_appl_market.margin_stock_detail (stock_code, trade_date)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.margin_account_stats
(
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    source text NOT NULL,
    financing_balance numeric(20,4),
    securities_lending_balance numeric(20,4),
    financing_buy_amount numeric(20,4),
    securities_lending_sell_amount numeric(20,4),
    broker_count integer,
    business_department_count integer,
    personal_investor_count integer,
    institution_investor_count integer,
    active_investor_count integer,
    liability_investor_count integer,
    total_guarantee numeric(20,4),
    average_guarantee_ratio numeric(20,6),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_margin_account_stats_active
    ON cynexus_appl_market.margin_account_stats (trade_date, source)
    WHERE deleted_at IS NULL;
