ALTER TABLE cynexus_appl_market.market_factor_study_sample
    ADD COLUMN IF NOT EXISTS outcome_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb;
