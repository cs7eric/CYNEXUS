CREATE INDEX IF NOT EXISTS idx_factor_snapshot_version_code_market_date
    ON cynexus_appl_market.a_stock_daily_factor_snapshot (feature_version, code, market, trade_date DESC)
    INCLUDE (available_at, amount, source, coverage, feature_payload);

CREATE INDEX IF NOT EXISTS idx_factor_membership_code_market_type_date
    ON cynexus_appl_market.market_factor_membership_snapshot (code, market, membership, effective_trade_date DESC)
    INCLUDE (industry_name, industry_code);
