\set ON_ERROR_STOP on

-- Keep titles focused on business meaning. Schedule windows, providers, and
-- implementation details remain available in the job description/config.
DO $$
DECLARE
    v_code text;
    v_label text;
    v_icon_hint text;
    v_sort_order integer;
    v_description text;
    v_category_id bigint;
BEGIN
    FOR v_code, v_label, v_icon_hint, v_sort_order, v_description IN
        SELECT *
          FROM (VALUES
            ('ai', 'AI 分析', 'sparkles', 10, '标的解读与专项分析任务。'),
            ('pre_market', '盘前准备', 'calendar', 20, '集合竞价和开盘前的数据准备任务。'),
            ('intraday', '盘中监控', 'activity', 30, '交易时段的行情、盘口与异动监控任务。'),
            ('market_snapshot', '市场快照', 'grid', 40, '面向市场页面的盘中、收盘和预热快照任务。'),
            ('fund_flow', '资金流', 'trending', 50, '个股、行业和大盘资金流采集及归档任务。'),
            ('market_sentiment', '市场情绪', 'activity', 60, '市场情绪指数及其因子计算任务。'),
            ('data_collection', '基础数据', 'database', 70, '行情、行业、股票资料和外部数据采集任务。'),
            ('margin', '两融数据', 'database', 80, '融资融券汇总、个股明细和历史覆盖任务。'),
            ('market_information', '市场资讯', 'file', 90, '媒体、官方资讯同步和资讯处理任务。'),
            ('eod_backfill', '盘后回填', 'refresh', 100, '盘后行情、复权和历史数据回填任务。'),
            ('quality_check', '质量检查', 'check', 110, '数据源一致性和采集结果质量检查任务。'),
            ('test', '测试', 'flask', 999, '仅用于验证调度器能力的测试任务。')
          ) AS defs(code, label, icon_hint, sort_order, description)
    LOOP
        SELECT id
          INTO v_category_id
          FROM cynexus_appl_core.scheduler_job_categories
         WHERE code = v_code
           AND deleted_at IS NULL
         ORDER BY id
         LIMIT 1;

        IF v_category_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910106000000000010)
              INTO v_category_id
              FROM cynexus_appl_core.scheduler_job_categories;

            INSERT INTO cynexus_appl_core.scheduler_job_categories (
                id, code, label, icon_hint, sort_order, description, status,
                remark, created_at, updated_at
            ) VALUES (
                v_category_id, v_code, v_label, v_icon_hint, v_sort_order,
                v_description, 'active',
                'migration:20260729_normalize_scheduler_job_catalog', now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_categories
               SET label = v_label,
                   icon_hint = v_icon_hint,
                   sort_order = v_sort_order,
                   description = v_description,
                   status = 'active',
                   deleted_at = NULL,
                   remark = 'migration:20260729_normalize_scheduler_job_catalog',
                   updated_at = now()
             WHERE id = v_category_id;
        END IF;
    END LOOP;
END $$;

UPDATE cynexus_appl_core.scheduler_jobs
   SET name = CASE code
       WHEN 'application_analysis' THEN '个股应用分析'
       WHEN 'a_stock_code_daily' THEN '自选股日频数据'
       WHEN 'a_stock_fund_flow_daily_qc' THEN '个股资金流同日质检'
       WHEN 'a_stock_fund_flow_history_backfill' THEN '个股资金流历史增量-全市场'
       WHEN 'a_stock_fund_flow_history_bootstrap_120' THEN '个股资金流历史补齐-120日'
       WHEN 'a_stock_fund_flow_intraday' THEN '个股资金流盘中采集'
       WHEN 'a_stock_fund_flow_limit_up_history' THEN '个股资金流历史增量-历史涨停池'
       WHEN 'a_stock_fund_flow_limit_up_qc' THEN '个股资金流历史涨停池质检'
       WHEN 'a_stock_fund_flow_remaining_qc' THEN '个股资金流全市场质检'
       WHEN 'a_stock_marketwide_daily' THEN '全市场日频数据'
       WHEN 'a_stock_research_weekly' THEN '自选股研报与预测'
       WHEN 'a_stock_tdx_realtime_probe' THEN '自选股盘口与逐笔成交'
       WHEN 'auction_ai_analysis' THEN '集合竞价 AI 分析'
       WHEN 'auction_closing_refresh' THEN '集合竞价尾盘采集'
       WHEN 'auction_opening_refresh' THEN '集合竞价早盘采集'
       WHEN 'eltdx_overview_close' THEN '大盘概览收盘快照'
       WHEN 'eltdx_overview_inside' THEN '大盘概览盘中快照'
       WHEN 'eltdx_overview_warmup' THEN '大盘概览开盘预热'
       WHEN 'initial_backfill_refresh' THEN '日线原始行情回填'
       WHEN 'kline_1m_intraday_realtime' THEN '指数1分钟行情盘中采集'
       WHEN 'kline_1m_tdx_close' THEN '指数1分钟行情归档'
       WHEN 'limit_emotion_refresh' THEN '涨跌停情绪因子'
       WHEN 'limit_up_fund_flow_minute_archive' THEN '历史涨停股分钟资金流归档'
       WHEN 'ma_count' THEN '均线与指数收益构建'
       WHEN 'ma_count_refresh' THEN '均线与指数收益刷新'
       WHEN 'margin_stock_history_backfill' THEN '个股两融历史回填'
       WHEN 'margin_sync_daily' THEN '两融数据日同步'
       WHEN 'market_etf_daily' THEN 'ETF 行情与份额采集'
       WHEN 'market_fund_flow_daily' THEN '大盘资金流采集'
       WHEN 'market_indicator_refresh' THEN '盘中异动监控'
       WHEN 'market_information_media_refresh' THEN '媒体资讯采集'
       WHEN 'market_information_official_refresh' THEN '官方资讯采集'
       WHEN 'market_information_pipeline_refresh' THEN '资讯智能处理'
       WHEN 'market_overview_close' THEN '市场概览收盘快照'
       WHEN 'market_overview_daily' THEN '市场概览日度回填'
       WHEN 'market_overview_inside' THEN '市场概览盘中快照'
       WHEN 'market_overview_warmup' THEN '市场概览开盘预热'
       WHEN 'market_pulse_close' THEN '市场脉冲收盘快照'
       WHEN 'market_pulse_constituents' THEN '市场脉冲行业成分更新'
       WHEN 'market_pulse_inside' THEN '市场脉冲盘中快照'
       WHEN 'market_sentiment_chain_refresh' THEN '市场情绪全链路'
       WHEN 'market_sentiment_index_refresh' THEN '市场情绪综合指数'
       WHEN 'profit_effect_refresh' THEN '赚钱效应因子'
       WHEN 'qfq_reconciliation_refresh' THEN '复权行情对账'
       WHEN 'risk_appetite_refresh' THEN '风险偏好因子'
       WHEN 'sector_breadth_refresh' THEN '行业扩散因子'
       WHEN 'stock_fund_flow_daily' THEN '个股资金流日快照'
       WHEN 'stock_universe_refresh' THEN '股票行业归属同步'
       WHEN 'style_risk_appetite_refresh' THEN '风格风险偏好因子'
       WHEN 'tdx_hsjday_download' THEN 'TDX 日线原始文件下载'
       WHEN 'tdx_industry_fund_flow_daily' THEN 'TDX 行业资金快照'
       WHEN 'test_scheduler_demo' THEN '测试任务'
       WHEN 'ths_industry_constituents_daily' THEN '同花顺行业成分日同步'
       WHEN 'ths_industry_constituents_weekly' THEN '同花顺行业成分周同步'
       WHEN 'ths_industry_fund_flow_daily' THEN '同花顺行业资金流采集'
       WHEN 'turnover' THEN '换手率盘中监控'
       WHEN 'turnover_activity_refresh' THEN '成交活跃度因子'
       WHEN 'turnover_refresh' THEN '全市场换手率采集'
       WHEN 'volatility_sentiment_refresh' THEN '波动率情绪因子'
       ELSE name
   END,
       remark = 'migration:20260729_normalize_scheduler_job_catalog',
       updated_at = now()
 WHERE deleted_at IS NULL
   AND code IN (
       'application_analysis', 'a_stock_code_daily', 'a_stock_fund_flow_daily_qc',
       'a_stock_fund_flow_history_backfill', 'a_stock_fund_flow_history_bootstrap_120',
       'a_stock_fund_flow_intraday', 'a_stock_fund_flow_limit_up_history',
       'a_stock_fund_flow_limit_up_qc', 'a_stock_fund_flow_remaining_qc',
       'a_stock_marketwide_daily', 'a_stock_research_weekly', 'a_stock_tdx_realtime_probe',
       'auction_ai_analysis', 'auction_closing_refresh', 'auction_opening_refresh',
       'eltdx_overview_close', 'eltdx_overview_inside', 'eltdx_overview_warmup',
       'initial_backfill_refresh', 'kline_1m_intraday_realtime', 'kline_1m_tdx_close',
       'limit_emotion_refresh', 'limit_up_fund_flow_minute_archive', 'ma_count',
       'ma_count_refresh', 'margin_stock_history_backfill', 'margin_sync_daily',
       'market_etf_daily', 'market_fund_flow_daily', 'market_indicator_refresh',
       'market_information_media_refresh', 'market_information_official_refresh',
       'market_information_pipeline_refresh', 'market_overview_close',
       'market_overview_daily', 'market_overview_inside', 'market_overview_warmup',
       'market_pulse_close', 'market_pulse_constituents', 'market_pulse_inside',
       'market_sentiment_chain_refresh', 'market_sentiment_index_refresh',
       'profit_effect_refresh', 'qfq_reconciliation_refresh', 'risk_appetite_refresh',
       'sector_breadth_refresh', 'stock_fund_flow_daily', 'stock_universe_refresh',
       'style_risk_appetite_refresh', 'tdx_hsjday_download', 'tdx_industry_fund_flow_daily',
       'test_scheduler_demo', 'ths_industry_constituents_daily',
       'ths_industry_constituents_weekly', 'ths_industry_fund_flow_daily', 'turnover',
       'turnover_activity_refresh', 'turnover_refresh', 'volatility_sentiment_refresh'
   );

DO $$
DECLARE
    v_job_code text;
    v_category_code text;
    v_job_id bigint;
    v_category_id bigint;
    v_mapping_id bigint;
BEGIN
    -- Rebuild the active catalog as one primary business category per job.
    UPDATE cynexus_appl_core.scheduler_job_category_mappings
       SET deleted_at = now(),
           updated_at = now(),
           remark = 'migration:20260729_normalize_scheduler_job_catalog'
     WHERE deleted_at IS NULL
       AND job_id IN (
           SELECT id
             FROM cynexus_appl_core.scheduler_jobs
            WHERE deleted_at IS NULL
       );

    FOR v_job_code, v_category_code IN
        SELECT *
          FROM (VALUES
            ('application_analysis', 'ai'),
            ('a_stock_code_daily', 'data_collection'),
            ('a_stock_fund_flow_daily_qc', 'quality_check'),
            ('a_stock_fund_flow_history_backfill', 'fund_flow'),
            ('a_stock_fund_flow_history_bootstrap_120', 'fund_flow'),
            ('a_stock_fund_flow_intraday', 'fund_flow'),
            ('a_stock_fund_flow_limit_up_history', 'fund_flow'),
            ('a_stock_fund_flow_limit_up_qc', 'quality_check'),
            ('a_stock_fund_flow_remaining_qc', 'quality_check'),
            ('a_stock_marketwide_daily', 'data_collection'),
            ('a_stock_research_weekly', 'data_collection'),
            ('a_stock_tdx_realtime_probe', 'intraday'),
            ('auction_ai_analysis', 'ai'),
            ('auction_closing_refresh', 'pre_market'),
            ('auction_opening_refresh', 'pre_market'),
            ('eltdx_overview_close', 'market_snapshot'),
            ('eltdx_overview_inside', 'market_snapshot'),
            ('eltdx_overview_warmup', 'market_snapshot'),
            ('initial_backfill_refresh', 'eod_backfill'),
            ('kline_1m_intraday_realtime', 'intraday'),
            ('kline_1m_tdx_close', 'data_collection'),
            ('limit_emotion_refresh', 'market_sentiment'),
            ('limit_up_fund_flow_minute_archive', 'fund_flow'),
            ('ma_count', 'market_sentiment'),
            ('ma_count_refresh', 'market_sentiment'),
            ('margin_stock_history_backfill', 'margin'),
            ('margin_sync_daily', 'margin'),
            ('market_etf_daily', 'data_collection'),
            ('market_fund_flow_daily', 'fund_flow'),
            ('market_indicator_refresh', 'intraday'),
            ('market_information_media_refresh', 'market_information'),
            ('market_information_official_refresh', 'market_information'),
            ('market_information_pipeline_refresh', 'market_information'),
            ('market_overview_close', 'market_snapshot'),
            ('market_overview_daily', 'eod_backfill'),
            ('market_overview_inside', 'market_snapshot'),
            ('market_overview_warmup', 'market_snapshot'),
            ('market_pulse_close', 'market_snapshot'),
            ('market_pulse_constituents', 'data_collection'),
            ('market_pulse_inside', 'market_snapshot'),
            ('market_sentiment_chain_refresh', 'market_sentiment'),
            ('market_sentiment_index_refresh', 'market_sentiment'),
            ('profit_effect_refresh', 'market_sentiment'),
            ('qfq_reconciliation_refresh', 'eod_backfill'),
            ('risk_appetite_refresh', 'market_sentiment'),
            ('sector_breadth_refresh', 'market_sentiment'),
            ('stock_fund_flow_daily', 'fund_flow'),
            ('stock_universe_refresh', 'data_collection'),
            ('style_risk_appetite_refresh', 'market_sentiment'),
            ('tdx_hsjday_download', 'data_collection'),
            ('tdx_industry_fund_flow_daily', 'fund_flow'),
            ('test_scheduler_demo', 'test'),
            ('ths_industry_constituents_daily', 'data_collection'),
            ('ths_industry_constituents_weekly', 'data_collection'),
            ('ths_industry_fund_flow_daily', 'fund_flow'),
            ('turnover', 'intraday'),
            ('turnover_activity_refresh', 'market_sentiment'),
            ('turnover_refresh', 'data_collection'),
            ('volatility_sentiment_refresh', 'market_sentiment')
          ) AS defs(job_code, category_code)
    LOOP
        SELECT j.id
          INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs j
         WHERE j.code = v_job_code
           AND j.deleted_at IS NULL
         ORDER BY j.id
         LIMIT 1;

        SELECT c.id
          INTO v_category_id
          FROM cynexus_appl_core.scheduler_job_categories c
         WHERE c.code = v_category_code
           AND c.deleted_at IS NULL
         ORDER BY c.id
         LIMIT 1;

        IF v_job_id IS NULL OR v_category_id IS NULL THEN
            CONTINUE;
        END IF;

        SELECT m.id
          INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings m
         WHERE m.job_id = v_job_id
           AND m.category_id = v_category_id
         ORDER BY m.deleted_at NULLS FIRST, m.updated_at DESC, m.id DESC
         LIMIT 1;

        IF v_mapping_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910107000000000500)
              INTO v_mapping_id
              FROM cynexus_appl_core.scheduler_job_category_mappings;

            INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
                id, job_id, category_id, remark, sort_order, created_at, updated_at
            ) VALUES (
                v_mapping_id, v_job_id, v_category_id,
                'migration:20260729_normalize_scheduler_job_catalog', 10, now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_category_mappings
               SET deleted_at = NULL,
                   remark = 'migration:20260729_normalize_scheduler_job_catalog',
                   sort_order = 10,
                   updated_at = now()
             WHERE id = v_mapping_id;
        END IF;
    END LOOP;
END $$;

