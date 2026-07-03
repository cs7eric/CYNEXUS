DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id
      INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'tdx_industry_fund_flow_daily'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000031)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'tdx_industry_fund_flow_daily',
            'TDX 56 行业资金快照',
            '每个交易日 17:00 触发 Python sync_tdx_industry_fund_flow，落库 PostgreSQL 供行业应用面分析页资金 Tab 调用。',
            'cynexus-data',
            'sync_tdx_industry_fund_flow',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'tdx_industry_fund_flow_daily',
                'data_job_key', 'sync_tdx_industry_fund_flow',
                'description', '每个交易日 17:00 同步 TDX 56 行业资金快照到 PostgreSQL',
                'schedule', jsonb_build_object('run_time', '17:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object('create_table', true)
            ),
            'migration:20260703_schedule_tdx_industry_fund_flow',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'TDX 56 行业资金快照',
               description = '每个交易日 17:00 触发 Python sync_tdx_industry_fund_flow，落库 PostgreSQL 供行业应用面分析页资金 Tab 调用。',
               service_module = 'cynexus-data',
               service_class = 'sync_tdx_industry_fund_flow',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'tdx_industry_fund_flow_daily',
                       'data_job_key', 'sync_tdx_industry_fund_flow',
                       'description', '每个交易日 17:00 同步 TDX 56 行业资金快照到 PostgreSQL',
                       'schedule', jsonb_build_object('run_time', '17:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                       'default_params', jsonb_build_object('create_table', true)
                   ),
               remark = 'migration:20260703_schedule_tdx_industry_fund_flow',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id
      INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000131)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;

        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds,
            extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id,
            v_job_id,
            TRUE,
            FALSE,
            '{"run_time":"17:00","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8,
            60,
            jsonb_build_object(
                'job_name', 'tdx_industry_fund_flow_daily',
                'lastDataJobKey', 'sync_tdx_industry_fund_flow',
                'lastMessage', 'scheduled daily TDX 56 industry fund-flow sync'
            ),
            '{}'::jsonb,
            'migration:20260703_schedule_tdx_industry_fund_flow',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = '{"run_time":"17:00","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'tdx_industry_fund_flow_daily',
                       'lastDataJobKey', 'sync_tdx_industry_fund_flow',
                       'lastMessage', 'scheduled daily TDX 56 industry fund-flow sync'
                   ),
               remark = 'migration:20260703_schedule_tdx_industry_fund_flow',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
