DO $$
DECLARE
    v_full_job_id bigint;
    v_priority_job_id bigint;
BEGIN
    SELECT id INTO v_full_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_refresh' AND deleted_at IS NULL
     LIMIT 1;

    IF v_full_job_id IS NOT NULL THEN
        UPDATE cynexus_appl_core.scheduler_jobs
           SET extra = jsonb_set(
                   COALESCE(extra, '{}'::jsonb),
                   '{default_params}',
                   COALESCE(extra->'default_params', '{}'::jsonb) || jsonb_build_object(
                       'batch_size', 240,
                       'quote_retry_attempts', 2,
                       'quote_retry_backoff_seconds', 0.25,
                       'quote_request_spacing_seconds', 0.02,
                       'quote_fallback_batch_size', 80
                   ),
                   true
               ),
               updated_at = now(),
               remark = 'migration:20260805_tune_market_indicator_quote_batches'
         WHERE id = v_full_job_id;
    END IF;

    SELECT id INTO v_priority_job_id
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'market_indicator_priority_refresh' AND deleted_at IS NULL
     LIMIT 1;

    IF v_priority_job_id IS NOT NULL THEN
        UPDATE cynexus_appl_core.scheduler_jobs
           SET extra = jsonb_set(
                   COALESCE(extra, '{}'::jsonb),
                   '{default_params}',
                   (COALESCE(extra, '{}'::jsonb)->'default_params') || jsonb_build_object(
                       'batch_size', 320,
                       'quote_retry_attempts', 2,
                       'quote_retry_backoff_seconds', 0.25,
                       'quote_request_spacing_seconds', 0.02,
                       'quote_fallback_batch_size', 80
                   ),
                   true
               ),
               updated_at = now(),
               remark = 'migration:20260805_tune_market_indicator_quote_batches'
         WHERE id = v_priority_job_id;
    END IF;
END $$;
