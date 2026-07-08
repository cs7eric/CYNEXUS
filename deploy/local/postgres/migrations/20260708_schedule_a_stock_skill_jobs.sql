DO $$
DECLARE
    v_job record;
    v_job_id bigint;
    v_status_id bigint;
BEGIN
    FOR v_job IN
        SELECT *
          FROM (
            VALUES
            (
                'a_stock_marketwide_daily',
                'A 股 Skill 全市场日频数据',
                '盘后同步行业排名、龙虎榜、涨跌停池、人气榜、全局资讯、同花顺热点、期权合约等全市场数据。',
                '{"run_time":"17:35","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
                '{"create_table":true,"ignore_errors":true,"datasets":["eastmoney_industry_rank","eastmoney_daily_dragon_tiger","eastmoney_limit_pool","eastmoney_hot_rank","eastmoney_global_news","ths_hot_reason","ths_limit_up_pool","ths_hot_list","sina_option_codes"],"pool":"zt","top":100,"top_n":30,"page_size":50}'::jsonb
            ),
            (
                'a_stock_code_daily',
                'A 股 Skill 自选股日频数据',
                '盘后按自选股同步东财基础信息、概念归属、120 日资金流、大宗交易、股东户数、分红、个股新闻、公告、互动易和新浪财报。',
                '{"run_time":"18:20","workday_only":true,"trading_day_only":true,"run_once_per_day":true}'::jsonb,
                '{"create_table":true,"ignore_errors":true,"code_source":"self_selected","limit_codes":30,"datasets":["eastmoney_stock_info","eastmoney_concept_blocks","eastmoney_fund_flow_120d","eastmoney_block_trade","eastmoney_holders","eastmoney_dividends","eastmoney_stock_news","eastmoney_hot_concept","cninfo_announcements","cninfo_irm","sina_financial_report"],"page_size":20,"num":8}'::jsonb
            ),
            (
                'a_stock_research_weekly',
                'A 股 Skill 研报与预测',
                '每周同步自选股东财研报、行业研报、iwencai 语义研报和同花顺 EPS 一致预期。',
                '{"run_time":"19:10","workday_only":true,"run_once_per_week":true}'::jsonb,
                '{"create_table":true,"ignore_errors":true,"code_source":"self_selected","limit_codes":20,"datasets":["eastmoney_reports","eastmoney_industry_reports","iwencai_search","ths_eps_forecast"],"max_pages":1,"query":"A股 研报 盈利预测","size":30}'::jsonb
            ),
            (
                'a_stock_fund_flow_intraday',
                'A 股 Skill 盘中资金流',
                '交易时段按自选股同步东财分钟级资金流到 ClickHouse，并同步同花顺北向分钟流向。',
                '{"every_minutes":15,"workday_only":true,"intraday_windows":[{"start":"09:35","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb,
                '{"create_table":true,"ignore_errors":true,"code_source":"self_selected","limit_codes":30,"datasets":["eastmoney_fund_flow_minute","ths_northbound"],"request_sleep_seconds":2}'::jsonb
            ),
            (
                'a_stock_tdx_realtime_probe',
                'A 股 Skill TDX 实时成交与盘口',
                '交易时段按自选股同步 mootdx 五档盘口和逐笔成交到 ClickHouse，用作盘中实时探针。',
                '{"every_minutes":10,"workday_only":true,"intraday_windows":[{"start":"09:35","end":"11:30"},{"start":"13:00","end":"15:05"}]}'::jsonb,
                '{"create_table":true,"ignore_errors":true,"code_source":"self_selected","limit_codes":20,"datasets":["tdx_order_book","tdx_transactions"],"request_sleep_seconds":0.2}'::jsonb
            )
          ) AS defs(code, name, description, schedule, default_params)
    LOOP
        SELECT id INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs
         WHERE code = v_job.code
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_job_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910110000000000100)
              INTO v_job_id
              FROM cynexus_appl_core.scheduler_jobs;
            INSERT INTO cynexus_appl_core.scheduler_jobs (
                id, code, name, description, service_module, service_class, config_file,
                is_enabled, registered_at, extra, remark, created_at, updated_at
            ) VALUES (
                v_job_id,
                v_job.code,
                v_job.name,
                v_job.description,
                'cynexus-data',
                'sync_a_stock_dataset',
                'jobs.yaml',
                TRUE,
                now(),
                jsonb_build_object(
                    'job_name', v_job.code,
                    'data_job_key', 'sync_a_stock_dataset',
                    'description', v_job.description,
                    'schedule', v_job.schedule,
                    'default_params', v_job.default_params
                ),
                'migration:20260708_schedule_a_stock_skill_jobs',
                now(),
                now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_jobs
               SET name = v_job.name,
                   description = v_job.description,
                   service_module = 'cynexus-data',
                   service_class = 'sync_a_stock_dataset',
                   config_file = 'jobs.yaml',
                   is_enabled = TRUE,
                   extra = COALESCE(extra, '{}'::jsonb)
                       || jsonb_build_object(
                           'job_name', v_job.code,
                           'data_job_key', 'sync_a_stock_dataset',
                           'description', v_job.description,
                           'schedule', v_job.schedule,
                           'default_params', v_job.default_params
                       ),
                   remark = 'migration:20260708_schedule_a_stock_skill_jobs',
                   updated_at = now()
             WHERE id = v_job_id;
        END IF;

        SELECT id INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses
         WHERE job_id = v_job_id
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_status_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910109000000000200)
              INTO v_status_id
              FROM cynexus_appl_core.scheduler_job_statuses;
            INSERT INTO cynexus_appl_core.scheduler_job_statuses (
                id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds,
                extra, progress, remark, created_at, updated_at
            ) VALUES (
                v_status_id,
                v_job_id,
                TRUE,
                FALSE,
                v_job.schedule,
                8,
                60,
                jsonb_build_object('job_name', v_job.code, 'lastDataJobKey', 'sync_a_stock_dataset', 'lastMessage', 'scheduled A-stock skill dataset sync'),
                '{}'::jsonb,
                'migration:20260708_schedule_a_stock_skill_jobs',
                now(),
                now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_statuses
               SET is_enabled = TRUE,
                   is_running = FALSE,
                   schedule = v_job.schedule,
                   timezone_offset_hours = 8,
                   tick_seconds = 60,
                   extra = COALESCE(extra, '{}'::jsonb) || jsonb_build_object('job_name', v_job.code, 'lastDataJobKey', 'sync_a_stock_dataset', 'lastMessage', 'scheduled A-stock skill dataset sync'),
                   remark = 'migration:20260708_schedule_a_stock_skill_jobs',
                   updated_at = now()
             WHERE id = v_status_id;
        END IF;
    END LOOP;
END $$;
