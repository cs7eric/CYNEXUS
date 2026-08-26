DO $$
DECLARE
    v_job_id bigint;
BEGIN
    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_refresh' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NULL THEN
        RAISE EXCEPTION 'market_indicator_refresh scheduler job is missing';
    END IF;

    UPDATE cynexus_appl_core.scheduler_jobs
       SET description = '交易时段每 30 秒全市场确认刷新；使用完整行业成分股确认板块传导并持久化回放事实。',
           extra = jsonb_build_object(
               'job_name', 'market_indicator_refresh',
               'data_job_key', 'market_indicator_refresh',
               'schedule', jsonb_build_object(
                   'every_seconds', 30, 'workday_only', true,
                   'intraday_windows', jsonb_build_array(
                       jsonb_build_object('start', '09:30', 'end', '11:30'),
                       jsonb_build_object('start', '13:00', 'end', '15:05')
                   )
               ),
               'default_params', jsonb_build_object(
                   'create_table', true, 'universe_mode', 'full_market',
                   'target_cadence_seconds', 30, 'pool_size', 8, 'batch_size', 240,
                   'quote_retry_attempts', 2, 'quote_retry_backoff_seconds', 0.25,
                   'quote_request_spacing_seconds', 0.02, 'quote_fallback_batch_size', 80,
                   'timeout', 8, 'rapid_change_pct', 1.0, 'max_gap_minutes', 5,
                   'signal_window_seconds', 5,
                   'publish_limit_board_discoveries', true,
                   'limit_board_redis_prefix', 'cynexus:market-indicator:limit-board:v1',
                   'limit_board_redis_ttl_seconds', 43200
               )
           ),
           updated_at = now(),
           remark = 'migration:20260805_schedule_market_indicator_refresh'
     WHERE id = v_job_id;

    UPDATE cynexus_appl_core.scheduler_job_statuses
       SET is_enabled = TRUE,
           is_running = FALSE,
           schedule = '{"every_seconds":30,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb,
           timezone_offset_hours = 8,
           tick_seconds = 30,
           updated_at = now(),
           remark = 'migration:20260805_schedule_market_indicator_refresh'
     WHERE job_id = v_job_id AND deleted_at IS NULL;
END $$;
