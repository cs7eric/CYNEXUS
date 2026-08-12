\set ON_ERROR_STOP on

BEGIN;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_trade_wall_clock_migration_marker (
    migration_key text PRIMARY KEY,
    applied_at timestamptz NOT NULL DEFAULT now()
);

-- Rebuild imported timestamps from the normalized screenshot value. Older
-- imports persisted the local wall-clock as UTC and a previous repair shifted
-- those instants in the wrong direction. Date-only markers remain unchanged.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM cynexus_appl_market.portfolio_trade_wall_clock_migration_marker
         WHERE migration_key = '20260810_wall_clock'
    ) THEN
UPDATE cynexus_appl_market.portfolio_trade trade
   SET traded_at = CASE
       WHEN candidate.raw_payload ->> 'tradedAt' ~ '^[0-9]{2}:[0-9]{2}(?::[0-9]{2}(?:\\.[0-9]{1,9})?)?$'
       THEN (((task.created_at AT TIME ZONE 'Asia/Shanghai')::date
              + (candidate.raw_payload ->> 'tradedAt')::time)
             AT TIME ZONE 'Asia/Shanghai')
       ELSE (
           regexp_replace(candidate.normalized_payload ->> 'tradedAt',
                          '(Z|[+-][0-9]{2}:[0-9]{2})$', '')::timestamp
           AT TIME ZONE 'Asia/Shanghai'
       )
   END,
       updated_at = now()
  FROM cynexus_appl_market.portfolio_import_candidate candidate
  JOIN cynexus_appl_market.portfolio_import_task task ON task.id = candidate.task_id
 WHERE candidate.id = trade.source_candidate_id
   AND trade.deleted_at IS NULL
   AND trade.status = 'ACTIVE'
   AND candidate.normalized_payload ->> 'tradedAt' IS NOT NULL
   AND (
       candidate.normalized_payload ->> 'tradedAt' !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}T00:00(?::00)?(?:Z|[+-]00:00)?$'
       OR candidate.raw_payload ->> 'tradedAt' ~ '^[0-9]{2}:[0-9]{2}(?::[0-9]{2}(?:\\.[0-9]{1,9})?)?$'
   );

        INSERT INTO cynexus_appl_market.portfolio_trade_wall_clock_migration_marker (migration_key)
        VALUES ('20260810_wall_clock');
    END IF;
END $$;

COMMIT;
