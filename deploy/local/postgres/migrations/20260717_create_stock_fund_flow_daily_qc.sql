CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_fund_flow_daily_qc (
    id BIGINT PRIMARY KEY,
    run_id TEXT NOT NULL,
    sample_date DATE NOT NULL,
    code TEXT NOT NULL,
    trade_date DATE,
    easy_tdx_source TEXT,
    eastmoney_source TEXT,
    easy_tdx_main_net NUMERIC,
    eastmoney_main_net NUMERIC,
    main_net_diff NUMERIC,
    main_net_diff_ratio NUMERIC,
    direction_match BOOLEAN,
    easy_tdx_payload JSONB,
    eastmoney_payload JSONB,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (run_id, code)
);

CREATE INDEX IF NOT EXISTS idx_a_stock_fund_flow_daily_qc_sample_date
    ON cynexus_appl_market.a_stock_fund_flow_daily_qc (sample_date DESC, code);

DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"run_time":"21:20","workday_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"create_table":true,"sample_size":50,"provider_timeout":15,"request_sleep_seconds":0}'::jsonb;
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
     WHERE code = 'a_stock_fund_flow_daily_qc'
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000180)
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'a_stock_fund_flow_daily_qc',
            '个股资金流同日质检',
            '无并发随机抽检全市场 50 只股票，对比 easy_tdx 与 EastMoney 同日个股资金流方向与量级差异。',
            'cynexus-data',
            'a_stock_fund_flow_daily_qc',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'a_stock_fund_flow_daily_qc',
                'data_job_key', 'a_stock_fund_flow_daily_qc',
                'description', '无并发随机抽检全市场 50 只股票，对比 easy_tdx 与 EastMoney 同日个股资金流方向与量级差异。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260717_create_stock_fund_flow_daily_qc',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '个股资金流同日质检',
               description = '无并发随机抽检全市场 50 只股票，对比 easy_tdx 与 EastMoney 同日个股资金流方向与量级差异。',
               service_module = 'cynexus-data',
               service_class = 'a_stock_fund_flow_daily_qc',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = jsonb_build_object(
                   'job_name', 'a_stock_fund_flow_daily_qc',
                   'data_job_key', 'a_stock_fund_flow_daily_qc',
                   'description', '无并发随机抽检全市场 50 只股票，对比 easy_tdx 与 EastMoney 同日个股资金流方向与量级差异。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260717_create_stock_fund_flow_daily_qc',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id
       AND deleted_at IS NULL
     LIMIT 1;

    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000280)
          INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object('job_name', 'a_stock_fund_flow_daily_qc', 'lastDataJobKey', 'a_stock_fund_flow_daily_qc'),
            '{}'::jsonb,
            'migration:20260717_create_stock_fund_flow_daily_qc',
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
                   || jsonb_build_object('job_name', 'a_stock_fund_flow_daily_qc', 'lastDataJobKey', 'a_stock_fund_flow_daily_qc'),
               remark = 'migration:20260717_create_stock_fund_flow_daily_qc',
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
        SELECT COALESCE(MAX(id) + 1, 910107000000000180)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id,
            v_job_id,
            v_category_id,
            'migration:20260717_create_stock_fund_flow_daily_qc',
            COALESCE((SELECT MAX(sort_order) + 10
                        FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id
                         AND deleted_at IS NULL), 10),
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_category_mappings
           SET deleted_at = NULL,
               remark = 'migration:20260717_create_stock_fund_flow_daily_qc',
               updated_at = now()
         WHERE id = v_mapping_id;
    END IF;
END $$;
