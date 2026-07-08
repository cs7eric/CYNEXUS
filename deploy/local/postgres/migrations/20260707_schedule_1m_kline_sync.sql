DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'kline_1m_intraday_realtime'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000081)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'kline_1m_intraday_realtime',
            '1mK 盘内实时同步',
            '交易时段每 5 分钟通过 Tencent/Mootdx 同步 Market Pulse 指数 1mK 到 ClickHouse。',
            'cynexus-data',
            'sync_kline_minute',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'kline_1m_intraday_realtime',
                'data_job_key', 'sync_kline_minute',
                'description', '盘内实时同步三大指数 1mK',
                'schedule', jsonb_build_object('every_minutes', 5, 'workday_only', true, 'intraday_windows', jsonb_build_array(jsonb_build_object('start', '09:30', 'end', '11:30'), jsonb_build_object('start', '13:00', 'end', '15:00'))),
                'default_params', jsonb_build_object('create_table', true, 'replace_existing', true, 'providers', jsonb_build_array('tencent', 'mootdx'), 'target_type', 'auto', 'period', 1, 'limit', 320, 'codes', jsonb_build_array('sh000001', 'sz399001', 'sz399006'))
            ),
            'migration:20260707_schedule_1m_kline_sync',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '1mK 盘内实时同步',
               description = '交易时段每 5 分钟通过 Tencent/Mootdx 同步 Market Pulse 指数 1mK 到 ClickHouse。',
               service_module = 'cynexus-data',
               service_class = 'sync_kline_minute',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'kline_1m_intraday_realtime',
                       'data_job_key', 'sync_kline_minute',
                       'description', '盘内实时同步三大指数 1mK',
                       'schedule', jsonb_build_object('every_minutes', 5, 'workday_only', true, 'intraday_windows', jsonb_build_array(jsonb_build_object('start', '09:30', 'end', '11:30'), jsonb_build_object('start', '13:00', 'end', '15:00'))),
                       'default_params', jsonb_build_object('create_table', true, 'replace_existing', true, 'providers', jsonb_build_array('tencent', 'mootdx'), 'target_type', 'auto', 'period', 1, 'limit', 320, 'codes', jsonb_build_array('sh000001', 'sz399001', 'sz399006'))
                   ),
               remark = 'migration:20260707_schedule_1m_kline_sync',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000181)
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
            '{"every_minutes":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
            8,
            60,
            jsonb_build_object('job_name', 'kline_1m_intraday_realtime', 'lastDataJobKey', 'sync_kline_minute', 'lastMessage', 'scheduled intraday Tencent/Mootdx 1mK sync'),
            '{}'::jsonb,
            'migration:20260707_schedule_1m_kline_sync',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = '{"every_minutes":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object('job_name', 'kline_1m_intraday_realtime', 'lastDataJobKey', 'sync_kline_minute', 'lastMessage', 'scheduled intraday Tencent/Mootdx 1mK sync'),
               remark = 'migration:20260707_schedule_1m_kline_sync',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'kline_1m_tdx_close'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000082)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'kline_1m_tdx_close',
            '1mK 盘后 TDX 本地归档',
            '交易日 17:45 解析本地通达信 1m 文件并写入 ClickHouse。',
            'cynexus-data',
            'sync_kline_minute',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'kline_1m_tdx_close',
                'data_job_key', 'sync_kline_minute',
                'description', '盘后解析本地 TDX 1mK',
                'schedule', jsonb_build_object('run_time', '17:45', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object('create_table', true, 'replace_existing', true, 'providers', jsonb_build_array('tdx'), 'target_type', 'auto', 'tdx_root', '/app/tdx')
            ),
            'migration:20260707_schedule_1m_kline_sync',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '1mK 盘后 TDX 本地归档',
               description = '交易日 17:45 解析本地通达信 1m 文件并写入 ClickHouse。',
               service_module = 'cynexus-data',
               service_class = 'sync_kline_minute',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'kline_1m_tdx_close',
                       'data_job_key', 'sync_kline_minute',
                       'description', '盘后解析本地 TDX 1mK',
                       'schedule', jsonb_build_object('run_time', '17:45', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                       'default_params', jsonb_build_object('create_table', true, 'replace_existing', true, 'providers', jsonb_build_array('tdx'), 'target_type', 'auto', 'tdx_root', '/app/tdx')
                   ),
               remark = 'migration:20260707_schedule_1m_kline_sync',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000182)
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
            '{"run_time":"17:45","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8,
            60,
            jsonb_build_object('job_name', 'kline_1m_tdx_close', 'lastDataJobKey', 'sync_kline_minute', 'lastMessage', 'scheduled close TDX local 1mK archive sync'),
            '{}'::jsonb,
            'migration:20260707_schedule_1m_kline_sync',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = '{"run_time":"17:45","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object('job_name', 'kline_1m_tdx_close', 'lastDataJobKey', 'sync_kline_minute', 'lastMessage', 'scheduled close TDX local 1mK archive sync'),
               remark = 'migration:20260707_schedule_1m_kline_sync',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;

