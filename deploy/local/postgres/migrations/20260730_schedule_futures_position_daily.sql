DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_schedule jsonb := '{"run_time":"19:00","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"providers":["akshare","sina"],"exchanges":["CFFEX","SHFE","CZCE","DCE","GFEX","INE"]}'::jsonb;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'futures_position_daily' AND deleted_at IS NULL
     LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000001000) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'futures_position_daily', '期货会员持仓监控',
            '交易所披露后同步期货会员成交量、多头和空头排名。', 'cynexus-data',
            'sync_futures_position', 'jobs.yaml', TRUE, now(),
            jsonb_build_object('job_name', 'futures_position_daily', 'data_job_key', 'sync_futures_position',
                               'schedule', v_schedule, 'default_params', v_params),
            'migration:20260730_schedule_futures_position_daily', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '期货会员持仓监控',
               description = '交易所披露后同步期货会员成交量、多头和空头排名。',
               service_module = 'cynexus-data', service_class = 'sync_futures_position',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = jsonb_build_object('job_name', 'futures_position_daily', 'data_job_key', 'sync_futures_position',
                                          'schedule', v_schedule, 'default_params', v_params),
               remark = 'migration:20260730_schedule_futures_position_daily', updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000001000) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'futures_position_daily', 'lastDataJobKey', 'sync_futures_position'),
            '{}'::jsonb, 'migration:20260730_schedule_futures_position_daily', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE, schedule = v_schedule,
               timezone_offset_hours = 8, tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('job_name', 'futures_position_daily', 'lastDataJobKey', 'sync_futures_position'),
               remark = 'migration:20260730_schedule_futures_position_daily', updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
