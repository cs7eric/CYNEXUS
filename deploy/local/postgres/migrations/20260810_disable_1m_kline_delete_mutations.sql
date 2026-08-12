-- ReplacingMergeTree(ingested_at) already provides the required idempotent
-- latest-row semantics for the intraday index collector.  Rewriting existing
-- rows via ALTER ... DELETE every five minutes creates three synchronous
-- mutations per run and repeatedly rewrites the same yearly partition.
UPDATE cynexus_appl_core.scheduler_jobs
   SET extra = jsonb_set(
           COALESCE(extra, '{}'::jsonb),
           '{default_params}',
           COALESCE(extra -> 'default_params', '{}'::jsonb)
               || jsonb_build_object('replace_existing', false),
           true
       ),
       remark = 'migration:20260810_disable_1m_kline_delete_mutations',
       updated_at = now()
 WHERE code IN ('kline_1m_intraday_realtime', 'kline_1m_tdx_close')
   AND deleted_at IS NULL;
