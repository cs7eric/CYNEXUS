UPDATE cynexus_appl_core.scheduler_jobs
   SET description = '交易日 17:45 增量解析本地通达信最新交易日及前 7 个自然日 1m 文件并写入 ClickHouse。',
       extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params}',
           COALESCE(extra -> 'default_params', '{}'::jsonb)
               || jsonb_build_object('incremental', true, 'lookback_days', 7),
           true
       ),
       remark = 'migration:20260806_extend_1m_tdx_incremental_window',
       updated_at = now()
 WHERE code = 'kline_1m_tdx_close'
   AND deleted_at IS NULL;
