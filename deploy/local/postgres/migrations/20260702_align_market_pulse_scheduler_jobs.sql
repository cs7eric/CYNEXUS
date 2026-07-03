DO $$
DECLARE
    v_job_id bigint;
BEGIN
    FOR v_job_id IN
        SELECT id
          FROM cynexus_appl_core.scheduler_jobs
         WHERE code IN (
             'market_overview_inside',
             'market_overview_close',
             'market_overview_warmup',
             'eltdx_overview_inside',
             'eltdx_overview_close',
             'eltdx_overview_warmup',
             'market_sentiment_chain_refresh',
             'market_overview_daily',
             'ths_industry_fund_flow_daily'
         )
           AND deleted_at IS NULL
    LOOP
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id,
            job_id,
            is_enabled,
            is_running,
            schedule,
            timezone_offset_hours,
            tick_seconds,
            extra,
            progress,
            remark,
            created_at,
            updated_at
        )
        SELECT
            COALESCE((SELECT MAX(id) + 1 FROM cynexus_appl_core.scheduler_job_statuses), 910109000000000101),
            v_job_id,
            TRUE,
            FALSE,
            '{}'::jsonb,
            8,
            60,
            '{}'::jsonb,
            '{}'::jsonb,
            'migration:20260702_align_market_pulse_scheduler_jobs',
            now(),
            now()
        WHERE NOT EXISTS (
            SELECT 1
              FROM cynexus_appl_core.scheduler_job_statuses s
             WHERE s.job_id = v_job_id
               AND s.deleted_at IS NULL
        );
    END LOOP;
END $$;

UPDATE cynexus_appl_core.scheduler_jobs
   SET service_module = 'cynexus-data',
       service_class = 'build_market_overview',
       config_file = 'jobs.yaml',
       extra = COALESCE(extra, '{}'::jsonb)
           || jsonb_build_object(
               'data_job_key', 'build_market_overview',
               'default_params', jsonb_build_object('create_table', true, 'source', 'akshare')
           ),
       updated_at = now(),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs'
 WHERE code IN ('market_overview_inside', 'market_overview_close', 'market_overview_warmup')
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_jobs
   SET service_module = 'cynexus-data',
       service_class = 'build_market_overview',
       config_file = 'jobs.yaml',
       extra = COALESCE(extra, '{}'::jsonb)
           || jsonb_build_object(
               'data_job_key', 'build_market_overview',
               'default_params', jsonb_build_object('create_table', true, 'source', 'eltdx')
           ),
       updated_at = now(),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs'
 WHERE code IN ('eltdx_overview_inside', 'eltdx_overview_close', 'eltdx_overview_warmup')
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_jobs
   SET service_module = 'cynexus-data',
       service_class = 'market_sentiment_chain_refresh',
       config_file = 'jobs.yaml',
       extra = COALESCE(extra, '{}'::jsonb)
           || jsonb_build_object(
               'data_job_key', 'market_sentiment_chain_refresh',
               'schedule', jsonb_build_object('run_time', '17:15', 'workday_only', true, 'run_once_per_day', true)
           ),
       updated_at = now(),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs'
 WHERE code = 'market_sentiment_chain_refresh'
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"every_minutes":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled intraday AKShare market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'market_overview_inside'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"cron":"35 15 * * mon-fri","workday_only":true}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled close AKShare market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'market_overview_close'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"run_time":"09:00","workday_only":true,"run_once_per_day":true}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled warmup AKShare market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'market_overview_warmup'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"every_minutes":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled intraday ELTDX market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'eltdx_overview_inside'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"cron":"35 15 * * mon-fri","workday_only":true}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled close ELTDX market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'eltdx_overview_close'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"run_time":"09:00","workday_only":true,"run_once_per_day":true}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'scheduled warmup ELTDX market overview snapshot refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'eltdx_overview_warmup'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET is_enabled = TRUE,
       is_running = FALSE,
       schedule = '{"run_time":"17:15","workday_only":true,"run_once_per_day":true}'::jsonb,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'market_sentiment_chain_refresh',
               'plannedSteps', 17,
               'lastMessage', 'scheduled daily market sentiment chain refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'market_sentiment_chain_refresh'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET schedule = '{}'::jsonb,
       is_running = FALSE,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'build_market_overview',
               'lastMessage', 'manual-only component step; scheduled via market_sentiment_chain_refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'market_overview_daily'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_job_statuses s
   SET schedule = '{}'::jsonb,
       is_running = FALSE,
       timezone_offset_hours = 8,
       tick_seconds = 60,
       extra = COALESCE(s.extra, '{}'::jsonb)
           || jsonb_build_object(
               'lastDataJobKey', 'sync_fund_flow',
               'lastMessage', 'manual-only component step; scheduled via market_sentiment_chain_refresh'
           ),
       remark = 'migration:20260702_align_market_pulse_scheduler_jobs',
       updated_at = now()
  FROM cynexus_appl_core.scheduler_jobs j
 WHERE s.job_id = j.id
   AND j.code = 'ths_industry_fund_flow_daily'
   AND j.deleted_at IS NULL
   AND s.deleted_at IS NULL;
