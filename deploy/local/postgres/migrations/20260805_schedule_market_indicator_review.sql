DO $$
DECLARE
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_outcome_finalize' AND deleted_at IS NULL
     LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000300) INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_outcome_finalize', 'Market Indicator 盘后结果评价',
            '盘后按事件时点回放板块 Episode，评价 5/15/30 分钟与收盘结果并沉淀失败原因。',
            'cynexus-data', 'market_indicator_outcome_finalize', 'jobs.yaml', TRUE, now(),
            jsonb_build_object(
                'job_name', 'market_indicator_outcome_finalize',
                'data_job_key', 'market_indicator_outcome_finalize',
                'schedule', jsonb_build_object('cron', '10 15 * * 1-5', 'workday_only', true),
                'default_params', jsonb_build_object('limit', 500)
            ),
            'migration:20260805_schedule_market_indicator_review', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET name = 'Market Indicator 盘后结果评价',
               description = '盘后按事件时点回放板块 Episode，评价 5/15/30 分钟与收盘结果并沉淀失败原因。',
               service_module = 'cynexus-data', service_class = 'market_indicator_outcome_finalize',
               config_file = 'jobs.yaml', is_enabled = TRUE,
               extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
                   'data_job_key', 'market_indicator_outcome_finalize',
                   'default_params', jsonb_build_object('limit', 500)
               ), updated_at = now(), remark = 'migration:20260805_schedule_market_indicator_review'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120000000000400) INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"cron":"10 15 * * 1-5","workday_only":true}'::jsonb,
            8, 60, jsonb_build_object('job_name', 'market_indicator_outcome_finalize', 'lastDataJobKey', 'market_indicator_outcome_finalize'),
            '{}'::jsonb, 'migration:20260805_schedule_market_indicator_review', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"cron":"10 15 * * 1-5","workday_only":true}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 60, updated_at = now(),
               remark = 'migration:20260805_schedule_market_indicator_review'
         WHERE id = v_status_id;
    END IF;
END $$;
