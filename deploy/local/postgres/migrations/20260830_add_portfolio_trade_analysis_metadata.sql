ALTER TABLE cynexus_appl_market.portfolio_trade
    ADD COLUMN IF NOT EXISTS strategy text,
    ADD COLUMN IF NOT EXISTS tags jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_portfolio_trade_analysis_strategy
    ON cynexus_appl_market.portfolio_trade (tenant_key, principal_key, strategy, traded_at)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';
