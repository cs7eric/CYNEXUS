\set ON_ERROR_STOP on

WITH extracted AS (
    SELECT candidate.id,
           (task.queued_at AT TIME ZONE 'Asia/Shanghai')::date AS import_date,
           COALESCE(
               CASE WHEN jsonb_typeof(candidate.raw_payload->'tradedAt') = 'object'
                    THEN candidate.raw_payload #>> '{tradedAt,value}'
                    ELSE candidate.raw_payload->>'tradedAt' END,
               CASE WHEN jsonb_typeof(candidate.raw_payload->'tradeTime') = 'object'
                    THEN candidate.raw_payload #>> '{tradeTime,value}'
                    ELSE candidate.raw_payload->>'tradeTime' END,
               CASE WHEN jsonb_typeof(candidate.raw_payload->'executionTime') = 'object'
                    THEN candidate.raw_payload #>> '{executionTime,value}'
                    ELSE candidate.raw_payload->>'executionTime' END,
               CASE WHEN jsonb_typeof(candidate.raw_payload->'time') = 'object'
                    THEN candidate.raw_payload #>> '{time,value}'
                    ELSE candidate.raw_payload->>'time' END
           ) AS trade_time
      FROM cynexus_appl_market.portfolio_import_candidate candidate
      JOIN cynexus_appl_market.portfolio_import_task task ON task.id = candidate.task_id
     WHERE task.queued_at IS NOT NULL
       AND (candidate.normalized_payload->>'tradedAt' IS NULL
            OR candidate.normalized_payload->>'tradedAt' ~ '^\d{2}:\d{2}(:\d{2})?$')
)
UPDATE cynexus_appl_market.portfolio_import_candidate candidate
   SET normalized_payload = jsonb_set(
           candidate.normalized_payload,
           '{tradedAt}',
           to_jsonb(to_char(extracted.import_date, 'YYYY-MM-DD') || 'T' || extracted.trade_time),
           true
       ),
       updated_at = now()
  FROM extracted
 WHERE candidate.id = extracted.id
   AND extracted.trade_time ~ '^\d{2}:\d{2}(:\d{2})?$';
