-- Protect newly imported timestamped trades from duplicate symbol/time inserts.
-- Legacy date-only fills are intentionally excluded because several historical
-- fills can share a midnight timestamp when the source image had no time.
CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_trade_owner_symbol_precise_time
    ON cynexus_appl_market.portfolio_trade (tenant_key, principal_key, symbol, traded_at)
    WHERE deleted_at IS NULL
      AND status = 'ACTIVE'
      AND traded_at <> (
          date_trunc('day', traded_at AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'
      );
