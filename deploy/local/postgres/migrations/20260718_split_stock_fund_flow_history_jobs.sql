DO $$
DECLARE
    v_limit_job_id bigint;
    v_limit_status_id bigint;
    v_remaining_job_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_limit_schedule jsonb := '{"run_time":"18:00","workday_only":true,"run_once_per_day":true}'::jsonb;
    v_remaining_schedule jsonb := '{"run_time":"00:00","workday_only":true,"run_once_per_day":true}'::jsonb;
    v_limit_params jsonb := '{"create_table":true,"run_all":true,"stock_scope":"limit-up-history","checkpoint_scope":"limit-up-history","fund_flow_workers":6,"fund_flow_count":5,"fund_flow_provider":"easy_tdx"}'::jsonb;
    v_remaining_params jsonb := '{"create_table":true,"run_all":true,"stock_scope":"remaining-active","checkpoint_scope":"remaining-active","fund_flow_workers":6,"fund_flow_count":5,"fund_flow_provider":"easy_tdx"}'::jsonb;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'data_collection'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category data_collection not found';
    END IF;

    SELECT id INTO v_remaining_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'a_stock_fund_flow_history_backfill'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_remaining_job_id IS NOT NULL THEN
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '个股资金流历史增量-剩余全市场',
               description = '次日 00:00 一次性同步非历史涨停股票池的近 5 日资金流，easy_tdx 为主源，EastMoney fallback。',
               service_module = 'cynexus-data',
               service_class = 'a_stock_fund_flow_history_backfill',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'a_stock_fund_flow_history_backfill',
                   'data_job_key', 'a_stock_fund_flow_history_backfill',
                   'description', '次日 00:00 一次性同步非历史涨停股票池的近 5 日资金流，easy_tdx 为主源，EastMoney fallback。',
                   'schedule', v_remaining_schedule,
                   'default_params', v_remaining_params
               ),
               remark = 'migration:20260718_split_stock_fund_flow_history_jobs',
               updated_at = now()
         WHERE id = v_remaining_job_id;

        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = v_remaining_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('job_name', 'a_stock_fund_flow_history_backfill', 'lastDataJobKey', 'a_stock_fund_flow_history_backfill'),
               remark = 'migration:20260718_split_stock_fund_flow_history_jobs',
               updated_at = now()
         WHERE job_id = v_remaining_job_id
           AND deleted_at IS NULL;
    END IF;

    SELECT id INTO v_limit_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'a_stock_fund_flow_limit_up_history'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_limit_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000190)
          INTO v_limit_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_limit_job_id,
            'a_stock_fund_flow_limit_up_history',
            '个股资金流历史增量-历史涨停池',
            '盘后 18:00 一次性同步历史涨停过股票池的近 5 日资金流，股票池来自 market_limit_daily_stocks 去重。',
            'cynexus-data',
            'a_stock_fund_flow_history_backfill',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'a_stock_fund_flow_limit_up_history',
                'data_job_key', 'a_stock_fund_flow_history_backfill',
                'description', '盘后 18:00 一次性同步历史涨停过股票池的近 5 日资金流，股票池来自 market_limit_daily_stocks 去重。',
                'schedule', v_limit_schedule,
                'default_params', v_limit_params
            ),
            'migration:20260718_split_stock_fund_flow_history_jobs',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '个股资金流历史增量-历史涨停池',
               description = '盘后 18:00 一次性同步历史涨停过股票池的近 5 日资金流，股票池来自 market_limit_daily_stocks 去重。',
               service_module = 'cynexus-data',
               service_class = 'a_stock_fund_flow_history_backfill',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'a_stock_fund_flow_limit_up_history',
                   'data_job_key', 'a_stock_fund_flow_history_backfill',
                   'description', '盘后 18:00 一次性同步历史涨停过股票池的近 5 日资金流，股票池来自 market_limit_daily_stocks 去重。',
                   'schedule', v_limit_schedule,
                   'default_params', v_limit_params
               ),
               remark = 'migration:20260718_split_stock_fund_flow_history_jobs',
               updated_at = now()
         WHERE id = v_limit_job_id;
    END IF;

    SELECT id INTO v_limit_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_limit_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_limit_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000290)
          INTO v_limit_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_limit_status_id, v_limit_job_id, TRUE, FALSE, v_limit_schedule, 8, 60,
            jsonb_build_object('job_name', 'a_stock_fund_flow_limit_up_history', 'lastDataJobKey', 'a_stock_fund_flow_history_backfill'),
            '{}'::jsonb,
            'migration:20260718_split_stock_fund_flow_history_jobs',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = v_limit_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('job_name', 'a_stock_fund_flow_limit_up_history', 'lastDataJobKey', 'a_stock_fund_flow_history_backfill'),
               remark = 'migration:20260718_split_stock_fund_flow_history_jobs',
               updated_at = now()
         WHERE id = v_limit_status_id;
    END IF;

    SELECT id INTO v_mapping_id
      FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_limit_job_id
       AND category_id = v_category_id
     ORDER BY deleted_at NULLS FIRST, updated_at DESC
     LIMIT 1;

    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910107000000000190)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id,
            v_limit_job_id,
            v_category_id,
            'migration:20260718_split_stock_fund_flow_history_jobs',
            COALESCE((SELECT MAX(sort_order) + 10
                        FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id
                         AND deleted_at IS NULL), 10),
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_category_mappings
           SET deleted_at = NULL,
               remark = 'migration:20260718_split_stock_fund_flow_history_jobs',
               updated_at = now()
         WHERE id = v_mapping_id;
    END IF;
END $$;
