DO $$
DECLARE
    v_job_id bigint;
    v_extra jsonb;
    v_params jsonb;
BEGIN
    SELECT id, extra INTO v_job_id, v_extra
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'a_stock_marketwide_daily' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NOT NULL THEN
        v_params := COALESCE(v_extra -> 'default_params', '{}'::jsonb)
            || jsonb_build_object(
                'datasets', jsonb_build_array(
                    'eastmoney_industry_rank', 'eastmoney_daily_dragon_tiger', 'eastmoney_limit_pool',
                    'eastmoney_hot_rank', 'eastmoney_global_news', 'ths_hot_reason', 'ths_limit_up_pool',
                    'ths_hot_list', 'sina_option_codes', 'cailian_telegraph', 'cninfo_market_announcements',
                    'cninfo_regulatory_announcements',
                    'official_market_information'
                ),
                'official_page_size', 20,
                'cailian_page_size', 50,
                'cninfo_market_page_size', 200
            );
        UPDATE cynexus_appl_core.scheduler_jobs
           SET description = '盘后同步行业排名、龙虎榜、涨跌停池、人气榜、全球资讯、财联社、官方宏观政策、交易所监管和行业消息。',
               extra = COALESCE(v_extra, '{}'::jsonb)
                   || jsonb_build_object(
                       'description', '盘后同步行业排名、龙虎榜、涨跌停池、人气榜、全球资讯、财联社、官方宏观政策、交易所监管和行业消息。',
                       'default_params', v_params
                   ),
               remark = 'migration:20260713_expand_market_information_sources',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;
END $$;
