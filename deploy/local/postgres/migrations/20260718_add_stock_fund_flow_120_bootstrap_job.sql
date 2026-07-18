DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{}'::jsonb;
    v_params jsonb := '{"create_table":true,"run_all":true,"shard_size":300,"stock_scope":"missing-120","checkpoint_scope":"missing-120","minimum_history_rows":120,"fund_flow_workers":6,"fund_flow_count":120,"fund_flow_provider":"easy_tdx","batch_sleep_seconds":30}'::jsonb;
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
     WHERE code = 'a_stock_fund_flow_history_bootstrap_120'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000220)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;

        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'a_stock_fund_flow_history_bootstrap_120',
            '个股资金流历史补齐-120日',
            '手动补齐 active 股票中资金流历史少于 120 日的标的；一次触发跑完整个缺口池，每 300 只休眠 30 秒，easy_tdx 为主源，EastMoney fallback。',
            'cynexus-data',
            'a_stock_fund_flow_history_backfill',
            'jobs.yaml',
            FALSE,
            now(),
            jsonb_build_object(
                'job_name', 'a_stock_fund_flow_history_bootstrap_120',
                'data_job_key', 'a_stock_fund_flow_history_backfill',
                'description', '手动补齐 active 股票中资金流历史少于 120 日的标的；一次触发跑完整个缺口池，每 300 只休眠 30 秒，easy_tdx 为主源，EastMoney fallback。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '个股资金流历史补齐-120日',
               description = '手动补齐 active 股票中资金流历史少于 120 日的标的；一次触发跑完整个缺口池，每 300 只休眠 30 秒，easy_tdx 为主源，EastMoney fallback。',
               service_module = 'cynexus-data',
               service_class = 'a_stock_fund_flow_history_backfill',
               config_file = 'jobs.yaml',
               is_enabled = FALSE,
               extra = jsonb_build_object(
                   'job_name', 'a_stock_fund_flow_history_bootstrap_120',
                   'data_job_key', 'a_stock_fund_flow_history_backfill',
                   'description', '手动补齐 active 股票中资金流历史少于 120 日的标的；一次触发跑完整个缺口池，每 300 只休眠 30 秒，easy_tdx 为主源，EastMoney fallback。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000320)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;

        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, FALSE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'a_stock_fund_flow_history_bootstrap_120', 'lastDataJobKey', 'a_stock_fund_flow_history_backfill'),
            '{}'::jsonb,
            'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = FALSE,
               is_running = FALSE,
               schedule = v_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('job_name', 'a_stock_fund_flow_history_bootstrap_120', 'lastDataJobKey', 'a_stock_fund_flow_history_backfill'),
               remark = 'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
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
        SELECT COALESCE(MAX(id) + 1, 910107000000000220)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;

        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id,
            v_job_id,
            v_category_id,
            'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
            COALESCE((SELECT MAX(sort_order) + 10
                        FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id
                         AND deleted_at IS NULL), 10),
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_category_mappings
           SET deleted_at = NULL,
               remark = 'migration:20260718_add_stock_fund_flow_120_bootstrap_job',
               updated_at = now()
         WHERE id = v_mapping_id;
    END IF;
END $$;
