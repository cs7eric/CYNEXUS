CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_backtest_run (
    id bigint PRIMARY KEY,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    strategy_version_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_version(id),
    requested_as_of_date date NOT NULL,
    actual_as_of_date date,
    status varchar(32) NOT NULL,
    config jsonb NOT NULL,
    benchmark varchar(32) NOT NULL,
    data_source text NOT NULL,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    started_at timestamptz NOT NULL,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_selection_backtest_status CHECK (status IN ('SUCCEEDED', 'NO_DATA', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_stock_selection_backtest_owner_recent
    ON cynexus_appl_market.stock_selection_strategy_backtest_run (tenant_id, owner_id, created_at DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_strategy_backtest_trade (
    id bigint PRIMARY KEY,
    backtest_run_id bigint NOT NULL REFERENCES cynexus_appl_market.stock_selection_strategy_backtest_run(id),
    signal_date date NOT NULL,
    entry_date date NOT NULL,
    exit_date date NOT NULL,
    market varchar(8) NOT NULL,
    stock_code varchar(16) NOT NULL,
    stock_name varchar(128),
    signal_rank integer NOT NULL,
    score numeric(18, 6) NOT NULL,
    entry_price numeric(20, 8) NOT NULL,
    exit_price numeric(20, 8) NOT NULL,
    gross_return_pct numeric(20, 8) NOT NULL,
    net_return_pct numeric(20, 8) NOT NULL,
    benchmark_return_pct numeric(20, 8) NOT NULL,
    excess_return_pct numeric(20, 8) NOT NULL,
    outcome varchar(16) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_selection_backtest_trade_outcome CHECK (outcome IN ('WIN', 'LOSS', 'FLAT'))
);
CREATE INDEX IF NOT EXISTS idx_stock_selection_backtest_trade_run_date
    ON cynexus_appl_market.stock_selection_strategy_backtest_trade (backtest_run_id, signal_date, signal_rank);
