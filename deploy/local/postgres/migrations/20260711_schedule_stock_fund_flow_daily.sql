DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_schedule jsonb := '{"run_time":"15:20","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"code_source":"self_selected","batch_size":120}'::jsonb;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'stock_fund_flow_daily' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000100) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'stock_fund_flow_daily',
            '个股资金流日快照',
            '收盘后同步自选股腾讯主动买卖、成交额、换手率和大小单占比快照。',
            'cynexus-data',
            'sync_stock_fund_flow',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'stock_fund_flow_daily',
                'data_job_key', 'sync_stock_fund_flow',
                'description', '收盘后同步自选股腾讯主动买卖、成交额、换手率和大小单占比快照。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260711_schedule_stock_fund_flow_daily',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '个股资金流日快照',
               description = '收盘后同步自选股腾讯主动买卖、成交额、换手率和大小单占比快照。',
               service_module = 'cynexus-data',
               service_class = 'sync_stock_fund_flow',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'stock_fund_flow_daily',
                   'data_job_key', 'sync_stock_fund_flow',
                   'description', '收盘后同步自选股腾讯主动买卖、成交额、换手率和大小单占比快照。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260711_schedule_stock_fund_flow_daily',
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
            jsonb_build_object('job_name', 'stock_fund_flow_daily', 'lastDataJobKey', 'sync_stock_fund_flow'),
            '{}'::jsonb,
            'migration:20260711_schedule_stock_fund_flow_daily',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = v_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('job_name', 'stock_fund_flow_daily', 'lastDataJobKey', 'sync_stock_fund_flow'),
               remark = 'migration:20260711_schedule_stock_fund_flow_daily',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
