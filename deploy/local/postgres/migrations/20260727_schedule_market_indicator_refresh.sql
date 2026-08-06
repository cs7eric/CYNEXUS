DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_refresh' AND deleted_at IS NULL
     LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000100) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_refresh', 'Market Indicator 盘中异动监控',
            '交易时段通过 eltdx 批量采集过去一年出现过涨停或跌停的去重全量股票，持久化快速拉升/打压等事件。',
            'cynexus-data', 'market_indicator_refresh', 'jobs.yaml', TRUE, now(),
            jsonb_build_object(
                'job_name', 'market_indicator_refresh',
                'data_job_key', 'market_indicator_refresh',
                'schedule', jsonb_build_object(
                    'every_minutes', 1, 'workday_only', true,
                    'intraday_windows', jsonb_build_array(
                        jsonb_build_object('start', '09:30', 'end', '11:30'),
                        jsonb_build_object('start', '13:00', 'end', '15:05')
                    )
                ),
                'default_params', jsonb_build_object('create_table', true, 'limit_history_days', 365, 'pool_size', 8, 'batch_size', 80, 'rapid_change_pct', 1.0, 'max_gap_minutes', 5)
            ),
            'migration:20260727_schedule_market_indicator_refresh', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'Market Indicator 盘中异动监控',
               description = '交易时段通过 eltdx 批量采集过去一年出现过涨停或跌停的去重全量股票，持久化快速拉升/打压等事件。',
               service_module = 'cynexus-data', service_class = 'market_indicator_refresh',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'data_job_key', 'market_indicator_refresh',
                   'default_params', jsonb_build_object('create_table', true, 'limit_history_days', 365, 'pool_size', 8, 'batch_size', 80, 'rapid_change_pct', 1.0, 'max_gap_minutes', 5)
               ), updated_at = now(), remark = 'migration:20260727_schedule_market_indicator_refresh'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000200) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"every_minutes":1,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb,
            8, 60, jsonb_build_object('job_name', 'market_indicator_refresh', 'lastDataJobKey', 'market_indicator_refresh'),
            '{}'::jsonb, 'migration:20260727_schedule_market_indicator_refresh', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"every_minutes":1,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 60, updated_at = now(),
               remark = 'migration:20260727_schedule_market_indicator_refresh'
         WHERE id = v_status_id;
    END IF;
END $$;
