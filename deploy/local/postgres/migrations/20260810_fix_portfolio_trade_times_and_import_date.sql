\set ON_ERROR_STOP on

BEGIN;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_trade_time_migration_marker (
    migration_key text PRIMARY KEY,
    applied_at timestamptz NOT NULL DEFAULT now()
);

-- Older imports parsed image-local timestamps as UTC. Convert those instants back
-- to Asia/Shanghai while keeping the UTC-midnight date-only marker unchanged.
UPDATE cynexus_appl_market.portfolio_trade
   SET traded_at = traded_at + interval '8 hours',
       updated_at = now()
 WHERE deleted_at IS NULL
   AND status = 'ACTIVE'
   -- Exact UTC midnight is the existing date-only marker. Leave it unchanged
   -- because multiple same-day fills may legitimately share that marker.
   AND traded_at AT TIME ZONE 'UTC' <> date_trunc('day', traded_at AT TIME ZONE 'UTC')
   AND NOT EXISTS (
       SELECT 1
         FROM cynexus_appl_market.portfolio_trade_time_migration_marker
        WHERE migration_key = '20260810_local_trade_time'
   );

INSERT INTO cynexus_appl_market.portfolio_trade_time_migration_marker (migration_key)
VALUES ('20260810_local_trade_time')
ON CONFLICT (migration_key) DO NOTHING;

COMMIT;
