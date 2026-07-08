UPDATE cynexus_appl_core.scheduler_jobs
   SET service_module = 'cynexus-data',
       service_class = 'build_market_pulse',
       config_file = 'jobs.yaml',
       extra = COALESCE(extra, '{}'::jsonb)
           || jsonb_build_object(
               'data_job_key', 'build_market_pulse',
               'default_params', jsonb_build_object('create_table', true)
           ),
       updated_at = now(),
       remark = 'migration:20260706_harden_market_scheduler_jobs'
 WHERE code IN ('market_pulse_inside', 'market_pulse_close')
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_jobs
   SET service_module = 'cynexus-data',
       service_class = 'sync_ths_industry_constituents',
       config_file = 'jobs.yaml',
       extra = COALESCE(extra, '{}'::jsonb)
           || jsonb_build_object(
               'data_job_key', 'sync_ths_industry_constituents',
               'default_params', jsonb_build_object('create_table', true)
           ),
       updated_at = now(),
       remark = 'migration:20260706_harden_market_scheduler_jobs'
 WHERE code = 'market_pulse_constituents'
   AND deleted_at IS NULL;

UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params}',
           COALESCE(extra -> 'default_params', '{}'::jsonb)
               || jsonb_build_object('continue_on_code_error', true),
           true
       ),
       updated_at = now(),
       remark = 'migration:20260706_harden_market_scheduler_jobs'
 WHERE code = 'turnover_refresh'
   AND deleted_at IS NULL;
