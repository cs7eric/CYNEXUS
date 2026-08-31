CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_account_cash_flow (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    occurred_at timestamptz NOT NULL,
    amount numeric(28,8) NOT NULL,
    flow_type text NOT NULL,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_portfolio_cash_flow_type CHECK (flow_type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_portfolio_cash_flow_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_portfolio_cash_flow_owner_time
    ON cynexus_appl_market.portfolio_account_cash_flow (tenant_key, principal_key, occurred_at);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_account_snapshot (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    snapshot_date date NOT NULL,
    total_assets numeric(28,8) NOT NULL,
    available_cash numeric(28,8) NOT NULL,
    market_value numeric(28,8) NOT NULL,
    net_deposit numeric(28,8) NOT NULL DEFAULT 0,
    net_withdrawal numeric(28,8) NOT NULL DEFAULT 0,
    daily_pnl numeric(28,8),
    daily_return numeric(20,12),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_key, principal_key, snapshot_date),
    CONSTRAINT ck_portfolio_snapshot_values CHECK (total_assets >= 0 AND available_cash >= 0 AND market_value >= 0)
);

ALTER TABLE cynexus_appl_market.portfolio_trade
    ADD COLUMN IF NOT EXISTS industry_code text,
    ADD COLUMN IF NOT EXISTS industry_name text;
CREATE INDEX IF NOT EXISTS idx_portfolio_trade_industry
    ON cynexus_appl_market.portfolio_trade (tenant_key, principal_key, industry_code, traded_at)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';
