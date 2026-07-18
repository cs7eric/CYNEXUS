DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"run_time":"17:45","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"limit":120}'::jsonb;
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
     WHERE code = 'market_fund_flow_daily'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000100)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'market_fund_flow_daily',
            '大盘资金流日采集',
            '交易日收盘后从东方财富采集大盘主力、超大单、大单、中单和小单净流入及占比。',
            'cynexus-data',
            'sync_market_fund_flow',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'market_fund_flow_daily',
                'data_job_key', 'sync_market_fund_flow',
                'description', '交易日收盘后从东方财富采集大盘主力、超大单、大单、中单和小单净流入及占比。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260716_schedule_market_fund_flow_daily',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '大盘资金流日采集',
               description = '交易日收盘后从东方财富采集大盘主力、超大单、大单、中单和小单净流入及占比。',
               service_module = 'cynexus-data',
               service_class = 'sync_market_fund_flow',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'market_fund_flow_daily',
                   'data_job_key', 'sync_market_fund_flow',
                   'description', '交易日收盘后从东方财富采集大盘主力、超大单、大单、中单和小单净流入及占比。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260716_schedule_market_fund_flow_daily',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000200)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'market_fund_flow_daily', 'lastDataJobKey', 'sync_market_fund_flow'),
            '{}'::jsonb,
            'migration:20260716_schedule_market_fund_flow_daily',
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
                   || jsonb_build_object('job_name', 'market_fund_flow_daily', 'lastDataJobKey', 'sync_market_fund_flow'),
               remark = 'migration:20260716_schedule_market_fund_flow_daily',
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
        SELECT COALESCE(MAX(id) + 1, 910107000000000100)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id,
            v_job_id,
            v_category_id,
            'migration:20260716_schedule_market_fund_flow_daily',
            COALESCE((SELECT MAX(sort_order) + 10
                        FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id
                         AND deleted_at IS NULL), 10),
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_category_mappings
           SET deleted_at = NULL,
               remark = 'migration:20260716_schedule_market_fund_flow_daily',
               updated_at = now()
         WHERE id = v_mapping_id;
    END IF;
END $$;
