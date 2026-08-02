\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE cynexus_appl_market.portfolio_trade
    ADD COLUMN IF NOT EXISTS instrument_type text;

UPDATE cynexus_appl_market.portfolio_trade
   SET instrument_type = 'STOCK'
 WHERE instrument_type IS NULL;

ALTER TABLE cynexus_appl_market.portfolio_trade
    ALTER COLUMN instrument_type SET DEFAULT 'STOCK',
    ALTER COLUMN instrument_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'ck_portfolio_trade_instrument_type'
           AND conrelid = 'cynexus_appl_market.portfolio_trade'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_market.portfolio_trade
            ADD CONSTRAINT ck_portfolio_trade_instrument_type
            CHECK (instrument_type IN ('STOCK', 'ETF'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_portfolio_trade_owner_instrument_symbol_time
    ON cynexus_appl_market.portfolio_trade (tenant_key, principal_key, instrument_type, symbol, traded_at, id)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';

COMMIT;
