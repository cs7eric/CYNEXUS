DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id
      INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'turnover_refresh'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000033)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;

        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'turnover_refresh',
            'A 股全市场换手率刷新',
            '每个交易日 16:00 触发 Python sync_stock_turnover，按全市场 active stock list 计算换手率并落库 PostgreSQL。',
            'cynexus-data',
            'sync_stock_turnover',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'turnover_refresh',
                'data_job_key', 'sync_stock_turnover',
                'description', '每个交易日 16:00 全市场刷新 stock_turnover_series_snapshot',
                'schedule', jsonb_build_object('run_time', '16:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object('create_table', true, 'target_type', 'stock', 'period', '1d', 'adjust', 'qfq', 'trading_day_only', true, 'limit', 260, 'source', 'clickhouse+eltdx', 'pool_size', 4, 'timeout', 8)
            ),
            'migration:20260630_schedule_stock_turnover_full_market',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'A 股全市场换手率刷新',
               description = '每个交易日 16:00 触发 Python sync_stock_turnover，按全市场 active stock list 计算换手率并落库 PostgreSQL。',
               service_module = 'cynexus-data',
               service_class = 'sync_stock_turnover',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'turnover_refresh',
                       'data_job_key', 'sync_stock_turnover',
                       'description', '每个交易日 16:00 全市场刷新 stock_turnover_series_snapshot',
                       'schedule', jsonb_build_object('run_time', '16:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                       'default_params', jsonb_build_object('create_table', true, 'target_type', 'stock', 'period', '1d', 'adjust', 'qfq', 'trading_day_only', true, 'limit', 260, 'source', 'clickhouse+eltdx', 'pool_size', 4, 'timeout', 8)
                   ),
               remark = 'migration:20260630_schedule_stock_turnover_full_market',
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
        SELECT COALESCE(MAX(id) + 1, 910109000000000019)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;

        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds,
            extra, remark, created_at, updated_at
        ) VALUES (
            v_status_id,
            v_job_id,
            TRUE,
            FALSE,
            '{"run_time":"16:00","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8,
            60,
            jsonb_build_object(
                'job_name', 'turnover_refresh',
                'lastDataJobKey', 'sync_stock_turnover',
                'lastMessage', 'scheduled daily full-market turnover refresh'
            ),
            'migration:20260630_schedule_stock_turnover_full_market',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = '{"run_time":"16:00","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'turnover_refresh',
                       'lastDataJobKey', 'sync_stock_turnover',
                       'lastMessage', 'scheduled daily full-market turnover refresh'
                   ),
               remark = 'migration:20260630_schedule_stock_turnover_full_market',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;

    UPDATE cynexus_appl_core.scheduler_jobs
       SET is_enabled = FALSE,
           remark = 'disabled duplicate turnover scheduler; use turnover_refresh',
           updated_at = now()
     WHERE code = 'turnover'
       AND deleted_at IS NULL
       AND id <> v_job_id;

    UPDATE cynexus_appl_core.scheduler_job_statuses status
       SET is_enabled = FALSE,
           is_running = FALSE,
           remark = 'disabled duplicate turnover scheduler; use turnover_refresh',
           updated_at = now()
      FROM cynexus_appl_core.scheduler_jobs job
     WHERE status.job_id = job.id
       AND job.code = 'turnover'
       AND job.deleted_at IS NULL
       AND status.deleted_at IS NULL;
END $$;
