\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_ai.ai_usage_bindings
   SET params = COALESCE(params, '{}'::jsonb)
       || '{"prompt_version":"v3","schema_version":"v2"}'::jsonb,
       remark = CASE capability
           WHEN 'market_information_event_analysis'
               THEN 'DeepSeek V4 Flash event-impact analysis using schema v2 and prompt v3.'
           ELSE 'DeepSeek V4 Flash market brief using schema v2 and prompt v3.'
       END,
       updated_at = now()
 WHERE capability IN ('market_information_event_analysis', 'market_information_daily_brief')
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params,market_information_content_backfill_limit}',
           CASE code
               WHEN 'market_information_media_refresh' THEN '6'::jsonb
               ELSE '30'::jsonb
           END,
           true
       ),
       updated_at = now()
 WHERE code IN ('market_information_media_refresh', 'market_information_official_refresh')
   AND deleted_at IS NULL;

COMMIT;
