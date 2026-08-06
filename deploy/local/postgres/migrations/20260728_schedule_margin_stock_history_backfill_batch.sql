DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_schedule jsonb := '{"run_time":"20:45","workday_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"mode":"stock_history","code_source":"all","limit_codes":0,"history_days":120,"page_size":120,"workers":8,"request_interval":0.35,"providers":["eastmoney"]}'::jsonb;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'margin_stock_history_backfill' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000300)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'margin_stock_history_backfill', '个股两融历史回填',
            '每天批量补齐全市场个股两融历史，优先使用东方财富明细，按历史覆盖不足的股票轮转。',
            'cynexus-data', 'sync_margin', 'jobs.yaml', TRUE, now(),
            jsonb_build_object(
                'job_name', 'margin_stock_history_backfill',
                'data_job_key', 'sync_margin',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260728_schedule_margin_stock_history_backfill_batch', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET is_enabled = TRUE,
               description = '每天批量补齐全市场个股两融历史，优先使用东方财富明细，按历史覆盖不足的股票轮转。',
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'job_name', 'margin_stock_history_backfill',
                   'data_job_key', 'sync_margin',
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
        SELECT COALESCE(MAX(id) + 1, 910109000000000300)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'margin_stock_history_backfill', 'lastDataJobKey', 'sync_margin'),
            '{}'::jsonb, 'migration:20260728_schedule_margin_stock_history_backfill_batch', now(), now()
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
