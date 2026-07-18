ALTER TABLE cynexus_appl_market.market_etf_catalog
    ADD COLUMN IF NOT EXISTS classification_source text NOT NULL DEFAULT 'legacy.name_keyword';

ALTER TABLE cynexus_appl_market.market_etf_spot_snapshot
    ADD COLUMN IF NOT EXISTS classification_source text NOT NULL DEFAULT 'legacy.name_keyword';

ALTER TABLE cynexus_appl_market.market_etf_daily
    ADD COLUMN IF NOT EXISTS classification_source text NOT NULL DEFAULT 'legacy.name_keyword';

COMMENT ON COLUMN cynexus_appl_market.market_etf_catalog.classification_source IS
    'Classification provenance: authoritative fund/index catalog or deterministic fallback rule.';

COMMENT ON COLUMN cynexus_appl_market.market_etf_spot_snapshot.classification_source IS
    'Classification provenance: authoritative fund/index catalog or deterministic fallback rule.';

COMMENT ON COLUMN cynexus_appl_market.market_etf_daily.classification_source IS
    'Classification provenance: authoritative fund/index catalog or deterministic fallback rule.';
