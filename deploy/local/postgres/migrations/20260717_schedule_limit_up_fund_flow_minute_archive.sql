DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"run_time":"15:20","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"ignore_errors":true,"shard_size":80,"batch_flush_successes":10,"max_runtime_seconds":1200,"request_sleep_seconds":0.5,"provider_timeout_seconds":8,"fetch_retries":1,"retry_sleep_min_seconds":2,"retry_sleep_max_seconds":6,"complete_after_time":"14:50:00","checkpoint_key":"a_stock.sync_limit_up_fund_flow_minute_archive.round_robin"}'::jsonb;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'data_collection'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category data_collection not found';
    END IF;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'limit_up_fund_flow_minute_archive'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000150)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'limit_up_fund_flow_minute_archive',
            '历史涨停股分钟资金流归档',
            '交易日收盘后按 checkpoint 分片采集历史涨停股票池的东财分钟资金流，并将最后一分钟归档为日级资金流 fallback。',
            'cynexus-data',
            'sync_limit_up_fund_flow_minute_archive',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'limit_up_fund_flow_minute_archive',
                'data_job_key', 'sync_limit_up_fund_flow_minute_archive',
                'description', '交易日收盘后按 checkpoint 分片采集历史涨停股票池的东财分钟资金流，并将最后一分钟归档为日级资金流 fallback。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '历史涨停股分钟资金流归档',
               description = '交易日收盘后按 checkpoint 分片采集历史涨停股票池的东财分钟资金流，并将最后一分钟归档为日级资金流 fallback。',
               service_module = 'cynexus-data',
               service_class = 'sync_limit_up_fund_flow_minute_archive',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'limit_up_fund_flow_minute_archive',
                   'data_job_key', 'sync_limit_up_fund_flow_minute_archive',
                   'description', '交易日收盘后按 checkpoint 分片采集历史涨停股票池的东财分钟资金流，并将最后一分钟归档为日级资金流 fallback。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000250)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'limit_up_fund_flow_minute_archive', 'lastDataJobKey', 'sync_limit_up_fund_flow_minute_archive'),
            '{}'::jsonb,
            'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
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
                   || jsonb_build_object('job_name', 'limit_up_fund_flow_minute_archive', 'lastDataJobKey', 'sync_limit_up_fund_flow_minute_archive'),
               remark = 'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id
      FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id
       AND category_id = v_category_id
     ORDER BY deleted_at NULLS FIRST, updated_at DESC
     LIMIT 1;

    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910107000000000150)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id,
            v_job_id,
            v_category_id,
            'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
            COALESCE((SELECT MAX(sort_order) + 10
                        FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id
                         AND deleted_at IS NULL), 10),
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_category_mappings
           SET deleted_at = NULL,
               remark = 'migration:20260717_schedule_limit_up_fund_flow_minute_archive',
               updated_at = now()
         WHERE id = v_mapping_id;
    END IF;
END $$;
