DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id
      INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'stock_universe_refresh'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000002)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'stock_universe_refresh',
            'A 股全市场 F10 归属同步',
            '每个交易日 17:00 触发 Python sync_stock_sectors，全量拉取股票题材/行业/风格归属并落库 PostgreSQL。',
            'cynexus-data',
            'sync_stock_sectors',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'stock_universe_refresh',
                'data_job_key', 'sync_stock_sectors',
                'description', '每个交易日 17:00 全量同步股票 F10 归属到 PostgreSQL',
                'schedule', jsonb_build_object('run_time', '17:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                'default_params', jsonb_build_object('create_table', true, 'trading_day_only', true, 'shard_size', 800, 'workers', 64, 'pool_size', 8, 'timeout', 6, 'max_retry_rounds', 5, 'batch_timeout_seconds', 180, 'sleep_first', 100, 'sleep_retry', 5)
            ),
            'migration:20260630_schedule_stock_universe_sectors',
            now(),
            now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'A 股全市场 F10 归属同步',
               description = '每个交易日 17:00 触发 Python sync_stock_sectors，全量拉取股票题材/行业/风格归属并落库 PostgreSQL。',
               service_module = 'cynexus-data',
               service_class = 'sync_stock_sectors',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'job_name', 'stock_universe_refresh',
                       'data_job_key', 'sync_stock_sectors',
                       'description', '每个交易日 17:00 全量同步股票 F10 归属到 PostgreSQL',
                       'schedule', jsonb_build_object('run_time', '17:00', 'workday_only', true, 'trading_day_only', true, 'run_once_per_day', true),
                       'default_params', jsonb_build_object('create_table', true, 'trading_day_only', true, 'shard_size', 800, 'workers', 64, 'pool_size', 8, 'timeout', 6, 'max_retry_rounds', 5, 'batch_timeout_seconds', 180, 'sleep_first', 100, 'sleep_retry', 5)
                   ),
               remark = 'migration:20260630_schedule_stock_universe_sectors',
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
        SELECT COALESCE(MAX(id) + 1, 910109000000000018)
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
                'job_name', 'stock_universe_refresh',
                'lastDataJobKey', 'sync_stock_sectors',
                'lastMessage', 'scheduled daily stock-sector universe sync'
            ),
            '{}'::jsonb,
            'migration:20260630_schedule_stock_universe_sectors',
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
                       'job_name', 'stock_universe_refresh',
                       'lastDataJobKey', 'sync_stock_sectors',
                       'lastMessage', 'scheduled daily stock-sector universe sync'
                   ),
               remark = 'migration:20260630_schedule_stock_universe_sectors',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;
END $$;
