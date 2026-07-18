DO $$
DECLARE
    v_params jsonb := '{"create_table":true,"code_source":"all","provider":"easy_tdx","batch_size":300}'::jsonb;
BEGIN
    UPDATE cynexus_appl_core.scheduler_jobs
       SET description = '收盘后使用 easy_tdx 同步全市场个股当前资金流，单股失败回退 EastMoney。',
           extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object('default_params', v_params),
           updated_at = now()
     WHERE code = 'stock_fund_flow_daily' AND deleted_at IS NULL;

    UPDATE cynexus_appl_core.scheduler_jobs
       SET service_class = 'a_stock_fund_flow_history_backfill',
           description = '使用 easy_tdx 回填全市场 120 日个股资金流，每批 300 只，按股票 checkpoint 断点续跑，失败仅重试未完成股票。',
           extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object(
               'data_job_key', 'a_stock_fund_flow_history_backfill',
               'default_params', '{"create_table":true,"shard_size":300,"fund_flow_workers":6,"fund_flow_count":120,"fund_flow_provider":"easy_tdx","checkpoint_scope":"all-active"}'::jsonb
           ),
           updated_at = now()
     WHERE code = 'a_stock_fund_flow_history_backfill' AND deleted_at IS NULL;

    UPDATE cynexus_appl_core.provider_capability_catalog
       SET provider_code = 'easy_tdx',
           provider_name = 'easy_tdx TDX/MAC',
           upstream_endpoints = '["easy_tdx TdxClient.get_history_fund_flow","EastMoney Push2His fallback"]'::jsonb,
           anti_scraping_notes = 'easy_tdx 主源；EastMoney Push2His 作为单股失败 fallback。',
           updated_at = now()
     WHERE capability_code = 'eastmoney_fund_flow_120d';
END $$;
