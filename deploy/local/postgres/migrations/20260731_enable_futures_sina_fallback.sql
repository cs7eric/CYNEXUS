UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
       COALESCE(extra, '{}'::jsonb),
       '{default_params}',
       '{"create_table":true,"providers":["akshare","sina"],"exchanges":["CFFEX","SHFE","CZCE","DCE","GFEX","INE"]}'::jsonb,
       true
   ),
       updated_at = now(),
       remark = 'migration:20260731_enable_futures_sina_fallback'
 WHERE code = 'futures_position_daily' AND deleted_at IS NULL;
