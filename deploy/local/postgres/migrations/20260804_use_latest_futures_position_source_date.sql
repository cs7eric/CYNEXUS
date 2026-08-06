UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
       COALESCE(extra, '{}'::jsonb),
       '{default_params}',
       '{"create_table":true,"providers":["akshare","sina"],"exchanges":["CFFEX","SHFE","CZCE","DCE","GFEX","INE"],"latest_source_data":true,"lookback_days":10}'::jsonb,
       true
   ),
       updated_at = now(),
       remark = 'migration:20260804_use_latest_futures_position_source_date'
 WHERE code = 'futures_position_daily' AND deleted_at IS NULL;
