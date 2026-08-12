DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id INTO v_job_id FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'trade_review_timeshare_close' AND deleted_at IS NULL LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000210) INTO v_job_id FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'trade_review_timeshare_close', '复盘分时选择性归档',
            '盘后按涨跌停、THS 行业强弱、交易记录和市场上下文选择性归档分时并发布 Redis。',
            'cynexus-data', 'sync_trade_review_timeshare', 'jobs.yaml', TRUE, now(),
            jsonb_build_object(
                'job_name', 'trade_review_timeshare_close',
                'data_job_key', 'sync_trade_review_timeshare',
                'schedule', jsonb_build_object('run_time', '18:15', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object('redis_cache', true, 'redis_ttl_seconds', 604800, 'ignore_errors', true)
            ), 'migration:20260809_schedule_trade_review_timeshare', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET is_enabled = TRUE, service_module = 'cynexus-data', service_class = 'sync_trade_review_timeshare',
               config_file = 'jobs.yaml', extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'job_name', 'trade_review_timeshare_close', 'data_job_key', 'sync_trade_review_timeshare',
                   'schedule', jsonb_build_object('run_time', '18:15', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                   'default_params', jsonb_build_object('redis_cache', true, 'redis_ttl_seconds', 604800, 'ignore_errors', true)
               ), remark = 'migration:20260809_schedule_trade_review_timeshare', updated_at = now()
         WHERE id = v_job_id;
    END IF;
    SELECT id INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000310) INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds,
            extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"run_time":"18:15","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8, 60, jsonb_build_object('job_name', 'trade_review_timeshare_close', 'lastDataJobKey', 'sync_trade_review_timeshare'),
            '{}'::jsonb, 'migration:20260809_schedule_trade_review_timeshare', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"run_time":"18:15","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object('job_name', 'trade_review_timeshare_close', 'lastDataJobKey', 'sync_trade_review_timeshare'),
               remark = 'migration:20260809_schedule_trade_review_timeshare', updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
