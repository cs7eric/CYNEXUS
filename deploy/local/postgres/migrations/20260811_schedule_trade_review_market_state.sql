DO $$
DECLARE v_job_id bigint; v_status_id bigint;
BEGIN
    SELECT id INTO v_job_id FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'trade_review_market_state_close' AND deleted_at IS NULL LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910110000000000220) INTO v_job_id FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs
            (id, code, name, description, service_module, service_class, config_file, is_enabled, registered_at, extra, remark, created_at, updated_at)
        VALUES (v_job_id, 'trade_review_market_state_close', '复盘市场状态物化',
            '盘后基于本地 TDX 1mk、日级事实和持久化 Indicator 事件构建分钟/日级回放状态。',
            'cynexus-data', 'build_trade_review_market_state', 'jobs.yaml', TRUE, now(),
            jsonb_build_object('data_job_key','build_trade_review_market_state',
                'schedule',jsonb_build_object('run_time','18:05','workday_only',true,'trading_day_only',true,'run_once_per_day',true),
                'default_params',jsonb_build_object('window_days',60,'create_table',true,'redis_cache',true,'redis_ttl_seconds',604800)),
            'migration:20260811_schedule_trade_review_market_state', now(), now());
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs SET is_enabled=TRUE, service_module='cynexus-data', service_class='build_trade_review_market_state',
            config_file='jobs.yaml', extra=COALESCE(extra,'{}'::jsonb) || jsonb_build_object('data_job_key','build_trade_review_market_state',
                'schedule',jsonb_build_object('run_time','18:05','workday_only',true,'trading_day_only',true,'run_once_per_day',true),
                'default_params',jsonb_build_object('window_days',60,'create_table',true,'redis_cache',true,'redis_ttl_seconds',604800)),
            remark='migration:20260811_schedule_trade_review_market_state', updated_at=now() WHERE id=v_job_id;
    END IF;
    SELECT id INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses WHERE job_id=v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910109000000000320) INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses
            (id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds, extra, progress, remark, created_at, updated_at)
        VALUES (v_status_id,v_job_id,TRUE,FALSE,'{"run_time":"18:05","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            8,60,jsonb_build_object('lastDataJobKey','build_trade_review_market_state'),'{}'::jsonb,
            'migration:20260811_schedule_trade_review_market_state',now(),now());
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses SET is_enabled=TRUE,is_running=FALSE,
            schedule='{"run_time":"18:05","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
            timezone_offset_hours=8,tick_seconds=60,extra=COALESCE(extra,'{}'::jsonb) || jsonb_build_object('lastDataJobKey','build_trade_review_market_state'),
            remark='migration:20260811_schedule_trade_review_market_state',updated_at=now() WHERE id=v_status_id;
    END IF;
END $$;
