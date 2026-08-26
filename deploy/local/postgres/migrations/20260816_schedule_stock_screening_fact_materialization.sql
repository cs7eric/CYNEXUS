DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"run_time":"17:55","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb;
    v_params jsonb := '{"source_table":"daily_raw","batch_size":2000}'::jsonb;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'data_collection' AND deleted_at IS NULL
     LIMIT 1;
    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category data_collection not found';
    END IF;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'stock_screening_fact_materialization' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000800) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id,
            'stock_screening_fact_materialization',
            '选股日频事实物化',
            '交易日 17:55 将 ClickHouse daily_raw 物化为可回放的 PostgreSQL 选股日频事实，并校验覆盖与源一致。',
            'cynexus-data',
            'materialize_stock_screening_facts',
            'jobs.yaml',
            TRUE,
            now(),
            jsonb_build_object(
                'job_name', 'stock_screening_fact_materialization',
                'data_job_key', 'materialize_stock_screening_facts',
                'description', 'daily_raw 到 a_stock_daily_screening_fact 的受限日频物化；覆盖不一致即失败。',
                'schedule', v_schedule,
                'default_params', v_params
            ),
            'migration:20260816_schedule_stock_screening_fact_materialization',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '选股日频事实物化',
               description = '交易日 17:55 将 ClickHouse daily_raw 物化为可回放的 PostgreSQL 选股日频事实，并校验覆盖与源一致。',
               service_module = 'cynexus-data',
               service_class = 'materialize_stock_screening_facts',
               config_file = 'jobs.yaml',
               is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'job_name', 'stock_screening_fact_materialization',
                   'data_job_key', 'materialize_stock_screening_facts',
                   'description', 'daily_raw 到 a_stock_daily_screening_fact 的受限日频物化；覆盖不一致即失败。',
                   'schedule', v_schedule,
                   'default_params', v_params
               ),
               remark = 'migration:20260816_schedule_stock_screening_fact_materialization',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL
     LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000900) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
            jsonb_build_object(
                'job_name', 'stock_screening_fact_materialization',
                'lastDataJobKey', 'materialize_stock_screening_facts',
                'lastMessage', 'scheduled daily stock screening fact materialization'
            ),
            '{}'::jsonb,
            'migration:20260816_schedule_stock_screening_fact_materialization',
            now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE,
               is_running = FALSE,
               schedule = v_schedule,
               timezone_offset_hours = 8,
               tick_seconds = 60,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'job_name', 'stock_screening_fact_materialization',
                   'lastDataJobKey', 'materialize_stock_screening_facts',
                   'lastMessage', 'scheduled daily stock screening fact materialization'
               ),
               remark = 'migration:20260816_schedule_stock_screening_fact_materialization',
               updated_at = now()
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id
      FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id AND category_id = v_category_id AND deleted_at IS NULL
     ORDER BY updated_at DESC LIMIT 1;
    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000001000) INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id, v_job_id, v_category_id,
            'migration:20260816_schedule_stock_screening_fact_materialization',
            COALESCE((SELECT MAX(sort_order) + 10 FROM cynexus_appl_core.scheduler_job_category_mappings
                      WHERE category_id = v_category_id AND deleted_at IS NULL), 10),
            now(), now()
        );
    END IF;
END $$;
