UPDATE cynexus_appl_core.scheduler_jobs
   SET description = '交易日 17:45 查询 ClickHouse 最近有效 1m 数据日期，并从其后增量解析本地通达信文件。',
       extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params}',
           (
               COALESCE(extra -> 'default_params', '{}'::jsonb) - 'lookback_days'
           ) || jsonb_build_object('incremental', true),
           true
       ),
       remark = 'migration:20260806_resume_1m_tdx_from_effective_date',
       updated_at = now()
 WHERE code = 'kline_1m_tdx_close'
   AND deleted_at IS NULL;
