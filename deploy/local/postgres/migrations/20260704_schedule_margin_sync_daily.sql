DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id
      INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'margin_sync_daily'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000041)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;

        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'margin_sync_daily',
            'A 股两融数据同步',
            '每个交易日 17:30 触发 Python sync_margin，同步 AkShare 两融汇总、个股明细和账户统计并落库 PostgreSQL。',
            'cynexus-data',
            'sync_margin',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'margin_sync_daily',
                'data_job_key', 'sync_margin',
                'description', '每个交易日 17:30 同步 AkShare 两融汇总、个股明细和账户统计',
                'schedule', jsonb_build_object('run_time', '17:30', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object(
                    'create_table', true,
                    'providers', jsonb_build_array('akshare'),
                    'datasets', jsonb_build_array('summary', 'detail', 'account'),
                    'lock_scope', 'akshare'
                )
            ),
            'migration:20260704_schedule_margin_sync_daily',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'A 股两融数据同步',
               description = '每个交易日 17:30 触发 Python sync_margin，同步 AkShare 两融汇总、个股明细和账户统计并落库 PostgreSQL。',
               service_module = 'cynexus-data',
               service_class = 'sync_margin',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'margin_sync_daily',
                       'data_job_key', 'sync_margin',
                       'description', '每个交易日 17:30 同步 AkShare 两融汇总、个股明细和账户统计',
                       'schedule', jsonb_build_object('run_time', '17:30', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                       'default_params', jsonb_build_object(
                           'create_table', true,
                           'providers', jsonb_build_array('akshare'),
                           'datasets', jsonb_build_array('summary', 'detail', 'account'),
                           'lock_scope', 'akshare'
                       )
                   ),
               remark = 'migration:20260704_schedule_margin_sync_daily',
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
        SELECT COALESCE(MAX(id) + 1, 910109000000000141)
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
            '{"run_time":"17:30","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8,
            60,
            jsonb_build_object(
                'job_name', 'margin_sync_daily',
                'lastDataJobKey', 'sync_margin',
                'lastMessage', 'scheduled daily AkShare margin sync'
            ),
            '{}'::jsonb,
            'migration:20260704_schedule_margin_sync_daily',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = '{"run_time":"17:30","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'margin_sync_daily',
                       'lastDataJobKey', 'sync_margin',
                       'lastMessage', 'scheduled daily AkShare margin sync'
                   ),
               progress = COALESCE(progress, '{}'::jsonb),
               remark = 'migration:20260704_schedule_margin_sync_daily',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
