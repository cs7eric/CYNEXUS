UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params}',
           COALESCE(extra -> 'default_params', '{}'::jsonb)
               || jsonb_build_object(
                   'providers', jsonb_build_array('exchange', 'akshare'),
                   'provider_priority', jsonb_build_array('exchange', 'akshare'),
                   'datasets', jsonb_build_array('summary', 'detail', 'account'),
                   'create_table', true,
                   'lock_scope', 'margin'
               ),
           true
       ),
       updated_at = now(),
       remark = 'migration:20260706_margin_exchange_first_datasource'
 WHERE code = 'margin_sync_daily'
   AND deleted_at IS NULL;
