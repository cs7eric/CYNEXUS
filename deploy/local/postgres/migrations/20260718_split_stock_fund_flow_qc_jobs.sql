DO $$
DECLARE
    v_category_id bigint;
    v_job_id bigint;
    v_status_id bigint;
    v_mapping_id bigint;
    v_code text;
    v_name text;
    v_desc text;
    v_schedule jsonb;
    v_params jsonb;
    v_base_job_id bigint;
    v_base_status_id bigint;
    v_base_mapping_id bigint;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'data_collection'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category data_collection not found';
    END IF;

    FOR v_code, v_name, v_desc, v_schedule, v_params, v_base_job_id, v_base_status_id, v_base_mapping_id IN
        SELECT *
          FROM (VALUES
            (
              'a_stock_fund_flow_limit_up_qc',
              '个股资金流质检-历史涨停池',
              '18:40 抽检历史涨停池 50 只股票，对比 easy_tdx 与 EastMoney 同日资金流。',
              '{"run_time":"18:40","workday_only":true,"run_once_per_day":true}'::jsonb,
              '{"create_table":true,"stock_scope":"limit-up-history","sample_size":50,"provider_timeout":15,"request_sleep_seconds":0}'::jsonb,
              910110000000000200::bigint,
              910109000000000300::bigint,
              910107000000000200::bigint
            ),
            (
              'a_stock_fund_flow_remaining_qc',
              '个股资金流质检-剩余全市场',
              '00:40 抽检非历史涨停股票池 50 只股票，对比 easy_tdx 与 EastMoney 同日资金流。',
              '{"run_time":"00:40","workday_only":true,"run_once_per_day":true}'::jsonb,
              '{"create_table":true,"stock_scope":"remaining-active","sample_size":50,"provider_timeout":15,"request_sleep_seconds":0}'::jsonb,
              910110000000000201::bigint,
              910109000000000301::bigint,
              910107000000000201::bigint
            )
          ) AS item(code, name, description, schedule, params, base_job_id, base_status_id, base_mapping_id)
    LOOP
        SELECT id INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs
         WHERE code = v_code
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_job_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, v_base_job_id)
              INTO v_job_id
              FROM cynexus_appl_core.scheduler_jobs;
            INSERT INTO cynexus_appl_core.scheduler_jobs (
                id, code, name, description, service_module, service_class, config_file,
                is_enabled, registered_at, extra, remark, created_at, updated_at
            ) VALUES (
                v_job_id,
                v_code,
                v_name,
                v_desc,
                'cynexus-data',
                'a_stock_fund_flow_daily_qc',
                'jobs.yaml',
                TRUE,
                now(),
                jsonb_build_object(
                    'job_name', v_code,
                    'data_job_key', 'a_stock_fund_flow_daily_qc',
                    'description', v_desc,
                    'schedule', v_schedule,
                    'default_params', v_params
                ),
                'migration:20260718_split_stock_fund_flow_qc_jobs',
                now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_jobs
               SET name = v_name,
                   description = v_desc,
                   service_module = 'cynexus-data',
                   service_class = 'a_stock_fund_flow_daily_qc',
                   config_file = 'jobs.yaml',
                   is_enabled = TRUE,
                   extra = jsonb_build_object(
                       'job_name', v_code,
                       'data_job_key', 'a_stock_fund_flow_daily_qc',
                       'description', v_desc,
                       'schedule', v_schedule,
                       'default_params', v_params
                   ),
                   remark = 'migration:20260718_split_stock_fund_flow_qc_jobs',
                   updated_at = now()
             WHERE id = v_job_id;
        END IF;

        SELECT id INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses
         WHERE job_id = v_job_id
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_status_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, v_base_status_id)
              INTO v_status_id
              FROM cynexus_appl_core.scheduler_job_statuses;
            INSERT INTO cynexus_appl_core.scheduler_job_statuses (
                id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
                tick_seconds, extra, progress, remark, created_at, updated_at
            ) VALUES (
                v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
                jsonb_build_object('job_name', v_code, 'lastDataJobKey', 'a_stock_fund_flow_daily_qc'),
                '{}'::jsonb,
                'migration:20260718_split_stock_fund_flow_qc_jobs',
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
                       || jsonb_build_object('job_name', v_code, 'lastDataJobKey', 'a_stock_fund_flow_daily_qc'),
                   remark = 'migration:20260718_split_stock_fund_flow_qc_jobs',
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
            SELECT COALESCE(MAX(id) + 1, v_base_mapping_id)
              INTO v_mapping_id
              FROM cynexus_appl_core.scheduler_job_category_mappings;
            INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
                id, job_id, category_id, remark, sort_order, created_at, updated_at
            ) VALUES (
                v_mapping_id,
                v_job_id,
                v_category_id,
                'migration:20260718_split_stock_fund_flow_qc_jobs',
                COALESCE((SELECT MAX(sort_order) + 10
                            FROM cynexus_appl_core.scheduler_job_category_mappings
                           WHERE category_id = v_category_id
                             AND deleted_at IS NULL), 10),
                now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_category_mappings
               SET deleted_at = NULL,
                   remark = 'migration:20260718_split_stock_fund_flow_qc_jobs',
                   updated_at = now()
             WHERE id = v_mapping_id;
        END IF;
    END LOOP;
END $$;
