ALTER TABLE cynexus_appl_market.portfolio_import_process_history
    ADD COLUMN IF NOT EXISTS events jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE cynexus_appl_market.portfolio_import_process_history
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

UPDATE cynexus_appl_market.portfolio_import_process_history
   SET events = jsonb_build_array(jsonb_build_object(
       'id', id,
       'attemptNo', attempt_no,
       'eventType', event_type,
       'fromStatus', from_status,
       'toStatus', to_status,
       'messageId', message_id,
       'correlationId', correlation_id,
       'providerCode', provider_code,
       'modelCode', model_code,
       'errorCode', error_code,
       'errorMessage', error_message,
       'details', details,
       'occurredAt', occurred_at,
       'durationMs', duration_ms
   ))
 WHERE events = '[]'::jsonb;

WITH expanded AS (
    SELECT history.id,
           history.task_id,
           history.principal_key,
           history.tenant_key,
           event,
           COALESCE(NULLIF(event ->> 'occurredAt', '')::timestamptz, history.occurred_at) AS event_time
      FROM cynexus_appl_market.portfolio_import_process_history history
      CROSS JOIN LATERAL jsonb_array_elements(
          CASE WHEN jsonb_typeof(history.events) = 'array' THEN history.events ELSE '[]'::jsonb END
      ) event
), grouped AS (
    SELECT task_id,
           principal_key,
           tenant_key,
           MIN(id) AS keep_id,
           jsonb_agg(event ORDER BY event_time, id) AS all_events
      FROM expanded
     GROUP BY task_id, principal_key, tenant_key
)
UPDATE cynexus_appl_market.portfolio_import_process_history history
   SET events = grouped.all_events,
       updated_at = now()
  FROM grouped
 WHERE history.id = grouped.keep_id;

WITH keepers AS (
    SELECT task_id, principal_key, tenant_key, MIN(id) AS keep_id
      FROM cynexus_appl_market.portfolio_import_process_history
     GROUP BY task_id, principal_key, tenant_key
)
DELETE FROM cynexus_appl_market.portfolio_import_process_history history
 USING keepers
 WHERE history.task_id = keepers.task_id
   AND history.principal_key = keepers.principal_key
   AND history.tenant_key = keepers.tenant_key
   AND history.id <> keepers.keep_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_process_history_task
    ON cynexus_appl_market.portfolio_import_process_history (tenant_key, principal_key, task_id);
