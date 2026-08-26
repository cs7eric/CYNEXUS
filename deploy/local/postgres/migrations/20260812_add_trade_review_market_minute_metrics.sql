ALTER TABLE IF EXISTS cynexus_appl_market.trade_review_market_minute_state
    ADD COLUMN IF NOT EXISTS promotion_rate numeric(12,6),
    ADD COLUMN IF NOT EXISTS promoted_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS median_change_pct numeric(12,6);
