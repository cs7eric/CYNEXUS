DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_schedule jsonb := '{"run_time":"20:30","workday_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"ignore_errors":false,"code_source":"fund_flow_history_missing","limit_codes":1,"datasets":["eastmoney_fund_flow_120d"],"fund_flow_mode":"history","request_sleep_seconds":5}'::jsonb;
BEGIN
    UPDATE cynexus_appl_core.scheduler_jobs
       SET extra = jsonb_set(
               COALESCE(extra, '{}'::jsonb),
               '{default_params,fund_flow_mode}',
               '"daily"'::jsonb,
               true
           ),
           updated_at = now()
     WHERE code = 'a_stock_code_daily' AND deleted_at IS NULL;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'a_stock_fund_flow_history_backfill' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000100) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'a_stock_fund_flow_history_backfill',
            '个股资金流历史回填',
            '每天低频选择一只历史不足的自选股，使用持久浏览器会话回填东方财富资金流历史。',
            'cynexus-data',
            'sync_a_stock_dataset',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'a_stock_fund_flow_history_backfill',
                'data_job_key', 'sync_a_stock_dataset',
                'description', '每天低频选择一只历史不足的自选股，使用持久浏览器会话回填东方财富资金流历史。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260711_schedule_stock_fund_flow_history_backfill',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'job_name', 'a_stock_fund_flow_history_backfill',
                   'data_job_key', 'sync_a_stock_dataset',
                   'description', '每天低频选择一只历史不足的自选股，使用持久浏览器会话回填东方财富资金流历史。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000200) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'a_stock_fund_flow_history_backfill', 'lastDataJobKey', 'sync_a_stock_dataset'),
            '{}'::jsonb,
            'migration:20260711_schedule_stock_fund_flow_history_backfill',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = v_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
