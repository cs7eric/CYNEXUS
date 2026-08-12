DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'intraday' AND deleted_at IS NULL
     ORDER BY id LIMIT 1;
    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category intraday not found';
    END IF;

    SELECT id INTO v_job_id FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_limit_board_capture' AND deleted_at IS NULL LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000800) INTO v_job_id FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_limit_board_capture', '涨跌停封单盘中采集',
            '交易时段每 5 秒采集当前涨跌停股票的买一卖一，原始样本仅暂存 Redis。',
            'cynexus-data', 'market_indicator_limit_board_capture', 'jobs.yaml', TRUE, now(),
            '{"job_name":"market_indicator_limit_board_capture","data_job_key":"market_indicator_limit_board_capture","schedule":{"every_seconds":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]},"default_params":{"discovery_interval_seconds":30,"batch_size":240,"pool_size":8,"quote_retry_attempts":2,"quote_retry_backoff_seconds":0.25,"quote_request_spacing_seconds":0.02,"quote_fallback_batch_size":80,"timeout":8,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200}}'::jsonb,
            'migration:20260811_schedule_market_indicator_limit_board', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '涨跌停封单盘中采集',
               description = '交易时段每 5 秒采集当前涨跌停股票的买一卖一，原始样本仅暂存 Redis。',
               service_module = 'cynexus-data', service_class = 'market_indicator_limit_board_capture',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = '{"job_name":"market_indicator_limit_board_capture","data_job_key":"market_indicator_limit_board_capture","schedule":{"every_seconds":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]},"default_params":{"discovery_interval_seconds":30,"batch_size":240,"pool_size":8,"quote_retry_attempts":2,"quote_retry_backoff_seconds":0.25,"quote_request_spacing_seconds":0.02,"quote_fallback_batch_size":80,"timeout":8,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200}}'::jsonb,
               updated_at = now(), remark = 'migration:20260811_schedule_market_indicator_limit_board'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000900) INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"every_seconds":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
            8, 5, '{"job_name":"market_indicator_limit_board_capture","lastDataJobKey":"market_indicator_limit_board_capture"}'::jsonb,
            '{}'::jsonb, 'migration:20260811_schedule_market_indicator_limit_board', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"every_seconds":5,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 5, updated_at = now(),
               remark = 'migration:20260811_schedule_market_indicator_limit_board'
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id AND category_id = v_category_id AND deleted_at IS NULL LIMIT 1;
    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000000900) INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (v_mapping_id, v_job_id, v_category_id,
            'migration:20260811_schedule_market_indicator_limit_board',
            COALESCE((SELECT MAX(sort_order) + 10 FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id AND deleted_at IS NULL), 10), now(), now());
    END IF;
END $$;

DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
BEGIN
    SELECT id INTO v_category_id FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'market_snapshot' AND deleted_at IS NULL ORDER BY id LIMIT 1;
    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category market_snapshot not found';
    END IF;

    SELECT id INTO v_job_id FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_limit_board_finalize' AND deleted_at IS NULL LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001000) INTO v_job_id FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_limit_board_finalize', '涨跌停封单收盘规整',
            '上午和下午交易时段结束后，将 Redis 样本规整为按交易日、时段、股票汇总的 PostgreSQL 记录。',
            'cynexus-data', 'market_indicator_limit_board_finalize', 'jobs.yaml', TRUE, now(),
            '{"job_name":"market_indicator_limit_board_finalize","data_job_key":"market_indicator_limit_board_finalize","schedule":{"every_seconds":60,"workday_only":true,"intraday_windows":[{"start":"11:30","end":"13:00"},{"start":"15:00","end":"15:30"}]},"default_params":{"sample_interval_seconds":5,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200,"source":"eltdx.quotes.best_quote"}}'::jsonb,
            'migration:20260811_schedule_market_indicator_limit_board', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = '涨跌停封单收盘规整',
               description = '上午和下午交易时段结束后，将 Redis 样本规整为按交易日、时段、股票汇总的 PostgreSQL 记录。',
               service_module = 'cynexus-data', service_class = 'market_indicator_limit_board_finalize',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = '{"job_name":"market_indicator_limit_board_finalize","data_job_key":"market_indicator_limit_board_finalize","schedule":{"every_seconds":60,"workday_only":true,"intraday_windows":[{"start":"11:30","end":"13:00"},{"start":"15:00","end":"15:30"}]},"default_params":{"sample_interval_seconds":5,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200,"source":"eltdx.quotes.best_quote"}}'::jsonb,
               updated_at = now(), remark = 'migration:20260811_schedule_market_indicator_limit_board'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001100) INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"every_seconds":60,"workday_only":true,"intraday_windows":[{"start":"11:30","end":"13:00"},{"start":"15:00","end":"15:30"}]}'::jsonb,
            8, 60, '{"job_name":"market_indicator_limit_board_finalize","lastDataJobKey":"market_indicator_limit_board_finalize"}'::jsonb,
            '{}'::jsonb, 'migration:20260811_schedule_market_indicator_limit_board', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"every_seconds":60,"workday_only":true,"intraday_windows":[{"start":"11:30","end":"13:00"},{"start":"15:00","end":"15:30"}]}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 60, updated_at = now(),
               remark = 'migration:20260811_schedule_market_indicator_limit_board'
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id AND category_id = v_category_id AND deleted_at IS NULL LIMIT 1;
    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001200) INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (v_mapping_id, v_job_id, v_category_id,
            'migration:20260811_schedule_market_indicator_limit_board',
            COALESCE((SELECT MAX(sort_order) + 10 FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id AND deleted_at IS NULL), 10), now(), now());
    END IF;
END $$;
