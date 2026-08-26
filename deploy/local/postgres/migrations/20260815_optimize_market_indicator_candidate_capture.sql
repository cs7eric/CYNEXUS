-- Candidate-pool validation resolves the latest market-indicator capture on every
-- Screener signal request.  The existing `(trade_date, code, captured_at)` index
-- cannot efficiently prove that a capture exists because the query first binds
-- `trade_date + captured_at`.  This partial index keeps that proof index-only and
-- avoids repeatedly scanning the 1-minute snapshot history.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_market_indicator_snapshots_capture_valid
    ON cynexus_appl_market.market_indicator_snapshots (trade_date, captured_at, code)
    WHERE deleted_at IS NULL AND last_price > 0;
