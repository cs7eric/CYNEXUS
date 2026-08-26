-- Run limit-board captures as one short sequential session. The scheduler
-- runtime polls due jobs every 30 seconds, so a five-second standalone job
-- cannot provide five-second samples and repeatedly launches full discovery.
DO $$
DECLARE
    v_category_id bigint;
    v_job_id bigint;
    v_status_id bigint;
    v_mapping_id bigint;
BEGIN
    SELECT id INTO v_category_id
      FROM cynexus_appl_core.scheduler_job_categories
     WHERE code = 'intraday' AND deleted_at IS NULL
     ORDER BY id LIMIT 1;
    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'Scheduler category intraday not found';
    END IF;

    UPDATE cynexus_appl_core.scheduler_jobs
       SET is_enabled = FALSE, updated_at = now(),
           remark = 'migration:20260813_limit_board_session_capture superseded by sequential session'
     WHERE code = 'market_indicator_limit_board_capture' AND deleted_at IS NULL;
    UPDATE cynexus_appl_core.scheduler_job_statuses status
       SET is_enabled = FALSE, is_running = FALSE, updated_at = now(),
           remark = 'migration:20260813_limit_board_session_capture superseded by sequential session'
      FROM cynexus_appl_core.scheduler_jobs job
     WHERE status.job_id = job.id
       AND job.code = 'market_indicator_limit_board_capture'
       AND status.deleted_at IS NULL AND job.deleted_at IS NULL;

    SELECT id INTO v_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_limit_board_session' AND deleted_at IS NULL
     LIMIT 1;
    IF v_job_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001300)
          INTO v_job_id FROM cynexus_appl_core.scheduler_jobs;
        INSERT INTO cynexus_appl_core.scheduler_jobs (
            id, code, name, description, service_module, service_class, config_file,
            is_enabled, registered_at, extra, remark, created_at, updated_at
        ) VALUES (
            v_job_id, 'market_indicator_limit_board_session', '涨跌停封板连续采样',
            '单次持有调度租约，连续采集涨跌停封单样本，避免调度器 30 秒轮询造成采样缺口。',
            'cynexus-data', 'market_indicator_limit_board_session', 'jobs.yaml', TRUE, now(),
            '{"job_name":"market_indicator_limit_board_session","data_job_key":"market_indicator_limit_board_session","schedule":{"every_seconds":300,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]},"default_params":{"duration_seconds":300,"target_cadence_seconds":3,"failure_backoff_seconds":2,"batch_size":80,"pool_size":8,"quote_retry_attempts":0,"quote_retry_backoff_seconds":0.25,"quote_request_spacing_seconds":0.02,"quote_fallback_batch_size":80,"timeout":8,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200}}'::jsonb,
            'migration:20260813_limit_board_session_capture', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_jobs
           SET is_enabled = TRUE,
               service_module = 'cynexus-data',
               service_class = 'market_indicator_limit_board_session',
               config_file = 'jobs.yaml',
               extra = '{"job_name":"market_indicator_limit_board_session","data_job_key":"market_indicator_limit_board_session","schedule":{"every_seconds":300,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]},"default_params":{"duration_seconds":300,"target_cadence_seconds":3,"failure_backoff_seconds":2,"batch_size":80,"pool_size":8,"quote_retry_attempts":0,"quote_retry_backoff_seconds":0.25,"quote_request_spacing_seconds":0.02,"quote_fallback_batch_size":80,"timeout":8,"redis_prefix":"cynexus:market-indicator:limit-board:v1","redis_ttl_seconds":43200}}'::jsonb,
               updated_at = now(), remark = 'migration:20260813_limit_board_session_capture'
         WHERE id = v_job_id;
    END IF;

    SELECT id INTO v_status_id
      FROM cynexus_appl_core.scheduler_job_statuses
     WHERE job_id = v_job_id AND deleted_at IS NULL LIMIT 1;
    IF v_status_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001400)
          INTO v_status_id FROM cynexus_appl_core.scheduler_job_statuses;
        INSERT INTO cynexus_appl_core.scheduler_job_statuses (
            id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
            tick_seconds, extra, progress, remark, created_at, updated_at
        ) VALUES (
            v_status_id, v_job_id, TRUE, FALSE,
            '{"every_seconds":300,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
            8, 300, '{"job_name":"market_indicator_limit_board_session","lastDataJobKey":"market_indicator_limit_board_session"}'::jsonb,
            '{}'::jsonb, 'migration:20260813_limit_board_session_capture', now(), now()
        );
    ELSE
        UPDATE cynexus_appl_core.scheduler_job_statuses
           SET is_enabled = TRUE, is_running = FALSE,
               schedule = '{"every_seconds":300,"workday_only":true,"intraday_windows":[{"start":"09:30","end":"11:30"},{"start":"13:00","end":"15:00"}]}'::jsonb,
               timezone_offset_hours = 8, tick_seconds = 300, updated_at = now(),
               remark = 'migration:20260813_limit_board_session_capture'
         WHERE id = v_status_id;
    END IF;

    SELECT id INTO v_mapping_id
      FROM cynexus_appl_core.scheduler_job_category_mappings
     WHERE job_id = v_job_id AND category_id = v_category_id AND deleted_at IS NULL LIMIT 1;
    IF v_mapping_id IS NULL THEN
        SELECT COALESCE(MAX(id) + 1, 910120001000001500)
          INTO v_mapping_id FROM cynexus_appl_core.scheduler_job_category_mappings;
        INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
            id, job_id, category_id, remark, sort_order, created_at, updated_at
        ) VALUES (
            v_mapping_id, v_job_id, v_category_id,
            'migration:20260813_limit_board_session_capture',
            COALESCE((SELECT MAX(sort_order) + 10 FROM cynexus_appl_core.scheduler_job_category_mappings
                       WHERE category_id = v_category_id AND deleted_at IS NULL), 10), now(), now()
        );
    END IF;

    -- Existing summaries do not retain enough raw samples to distinguish a
    -- sealed interval from a provider outage. Keep their range as historical
    -- context, but make the quality limitation explicit to the API/UI.
    WITH target_rows AS (
        SELECT summary.id,
               COALESCE((summary.trend_features->>'sample_interval_seconds')::integer, 5) AS sample_interval_seconds,
               summary.is_sealed_at_close
          FROM cynexus_appl_market.market_indicator_limit_board_session_summaries summary
         WHERE summary.deleted_at IS NULL
           AND (
               summary.algorithm_version <> 'v3'
               OR summary.trend_features->>'quality_status' = 'historical_partial'
               OR summary.trend_features->>'duration_estimation' = 'historical_v2_range'
           )
    ), point_rows AS (
        SELECT summary.id,
               target.sample_interval_seconds,
               summary.is_sealed_at_close,
               (point.value->>0)::numeric AS elapsed_seconds,
               LEAD((point.value->>0)::numeric) OVER (PARTITION BY summary.id ORDER BY point.ordinality) AS next_elapsed_seconds
          FROM target_rows target
          JOIN cynexus_appl_market.market_indicator_limit_board_session_summaries summary ON summary.id = target.id
          CROSS JOIN LATERAL jsonb_array_elements(
              CASE WHEN jsonb_typeof(summary.trend_features->'sealed_amount_points') = 'array'
                   THEN summary.trend_features->'sealed_amount_points' ELSE '[]'::jsonb END
          ) WITH ORDINALITY AS point(value, ordinality)
    ), duration_rows AS (
        SELECT id,
               SUM(CASE WHEN next_elapsed_seconds > elapsed_seconds
                              AND next_elapsed_seconds - elapsed_seconds <= GREATEST(15, sample_interval_seconds * 3)
                        THEN sample_interval_seconds ELSE 0 END)
                 + CASE WHEN BOOL_OR(is_sealed_at_close) THEN MAX(sample_interval_seconds) ELSE 0 END AS duration_seconds
          FROM point_rows
         GROUP BY id
    )
    UPDATE cynexus_appl_market.market_indicator_limit_board_session_summaries summary
       SET sealed_duration_seconds = COALESCE(
               duration_rows.duration_seconds,
               CASE WHEN target_rows.is_sealed_at_close THEN target_rows.sample_interval_seconds ELSE 0 END,
               0
           )::integer,
           trend_features = COALESCE(trend_features, '{}'::jsonb) || jsonb_build_object(
               'duration_is_partial', TRUE,
               'quality_status', 'historical_partial',
               'duration_estimation', 'observed_contiguous_sample_windows',
               'duration_basis', 'nominal_sample_windows'
           ),
           algorithm_version = 'v3',
           updated_at = now()
      FROM target_rows
      LEFT JOIN duration_rows ON duration_rows.id = target_rows.id
     WHERE summary.id = target_rows.id;
END $$;

-- Keep the chart payload compact: two paired arrays instead of a JSON object
-- for every sampled point. Existing summaries remain queryable; new session
-- finalizations write the compact columns.
ALTER TABLE cynexus_appl_market.market_indicator_limit_board_session_summaries
    ADD COLUMN IF NOT EXISTS trend_series_origin_at timestamptz,
    ADD COLUMN IF NOT EXISTS trend_time_offsets smallint[] NOT NULL DEFAULT '{}'::smallint[],
    ADD COLUMN IF NOT EXISTS trend_sealed_amounts bigint[] NOT NULL DEFAULT '{}'::bigint[];

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'ck_market_indicator_limit_board_summary_trend_series'
           AND conrelid = 'cynexus_appl_market.market_indicator_limit_board_session_summaries'::regclass
    ) THEN
        ALTER TABLE cynexus_appl_market.market_indicator_limit_board_session_summaries
            ADD CONSTRAINT ck_market_indicator_limit_board_summary_trend_series
            CHECK (cardinality(trend_time_offsets) = cardinality(trend_sealed_amounts)
                AND cardinality(trend_time_offsets) <= 120);
    END IF;
END $$;
