DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"every_seconds":10,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb;
    v_params jsonb := '{"universe_mode":"historical_limit","limit_codes":1500,"limit_history_days":365,"pool_size":8,"batch_size":320,"quote_retry_attempts":2,"quote_retry_backoff_seconds":0.25,"quote_request_spacing_seconds":0.02,"quote_fallback_batch_size":80,"timeout":8,"target_cadence_seconds":10,"redis_prefix":"cynexus:market-indicator:priority","persist_postgres":false,"full_confirmation_required":true,"scope":"priority"}'::jsonb;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'intraday' AND deleted_at IS NULL
     LIMIT 1;
    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category intraday not found';
    END IF;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_priority_refresh' AND deleted_at IS NULL
     LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000500) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_priority_refresh', 'Market Indicator 盘中快速雷达',
            '交易时段每 10 秒刷新历史涨跌停关注池，仅发布 Redis 快线；不代表完整行业确认。',
            'cynexus-data', 'market_indicator_priority_refresh', 'jobs.yaml', TRUE, now(),
            jsonb_build_object('job_name','market_indicator_priority_refresh','data_job_key','market_indicator_priority_refresh','schedule',v_schedule,'default_params',v_params),
            'migration:20260805_schedule_market_indicator_priority_refresh', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'Market Indicator 盘中快速雷达',
               description = '交易时段每 10 秒刷新历史涨跌停关注池，仅发布 Redis 快线；不代表完整行业确认。',
               service_module = 'cynexus-data', service_class = 'market_indicator_priority_refresh',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = jsonb_build_object('job_name','market_indicator_priority_refresh','data_job_key','market_indicator_priority_refresh','schedule',v_schedule,'default_params',v_params),
               updated_at = now(), remark = 'migration:20260805_schedule_market_indicator_priority_refresh'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL
     LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000600) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 10,
            jsonb_build_object('job_name','market_indicator_priority_refresh','lastDataJobKey','market_indicator_priority_refresh'),
            '{}'::jsonb, 'migration:20260805_schedule_market_indicator_priority_refresh', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE, schedule = v_schedule,
               timezone_offset_hours = 8, tick_seconds = 10, updated_at = now(),
               remark = 'migration:20260805_schedule_market_indicator_priority_refresh'
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id
      FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id AND category_id = v_category_id AND deleted_at IS NULL
     ORDER BY updated_at DESC LIMIT 1;
    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000700)
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id, v_job_id, v_category_id,
            'migration:20260805_schedule_market_indicator_priority_refresh',
            COALESCE((SELECT MAX(sort_order) + 10 FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id AND deleted_at IS NULL), 10), now(), now()
        );
    END IF;
END $$;
