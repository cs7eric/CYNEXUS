\set ON_ERROR_STOP on

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_data_source_table_metadata (
    source_type varchar(32) NOT NULL,
    schema_name varchar(128) NOT NULL DEFAULT '',
    table_name varchar(128) NOT NULL,
    business_logic text,
    used_by jsonb NOT NULL DEFAULT '[]'::jsonb,
    update_job varchar(256),
    data_source varchar(256),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (source_type, schema_name, table_name)
);

INSERT INTO cynexus_appl_market.mkt_data_source_table_metadata
    (source_type, schema_name, table_name, business_logic, used_by, update_job, data_source)
VALUES
    ('postgres', '', 'a_stock_announcement', '巨潮公告检索结果表，保存自选股公告标题、类别、发布日期、下载链接和原始返回。', '["Stock Detail","Research Workspace","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'CNInfo / 巨潮资讯'),
    ('postgres', '', 'a_stock_block_trade', '东财大宗交易历史表，用于观察个股折溢价交易、买卖营业部和大额成交行为。', '["Stock Detail","Risk Review","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Datacenter RPT_DATA_BLOCKTRADE'),
    ('postgres', '', 'a_stock_concept_membership', '东财个股概念归属快照，用于个股题材标签、概念命中和题材聚合。', '["Stock Detail","Concept Analysis","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Push2 slist'),
    ('postgres', '', 'a_stock_dividend', '东财分红送转历史表，用于公司行动、除权除息和长期收益解释。', '["Stock Detail","Corporate Actions","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Datacenter RPT_SHAREBONUS_DET'),
    ('postgres', '', 'a_stock_dragon_tiger_daily', '东财龙虎榜个股日明细，用于追踪上榜原因、净买额、买卖额和换手率。', '["Market Review","Stock Detail","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily / a_stock_code_daily', 'EastMoney Datacenter RPT_DAILYBILLBOARD_DETAILSNEW'),
    ('postgres', '', 'a_stock_dragon_tiger_seat', '东财龙虎榜席位明细，用于分析营业部买卖方向、席位排名和机构净买卖。', '["Market Review","Stock Detail","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Datacenter billboard seat APIs'),
    ('postgres', '', 'a_stock_financial_statement', '新浪财务报表结构化字段表，用于个股利润表、资产负债表等基础财务展示。', '["Stock Detail","Fundamental Analysis","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'Sina CompanyFinanceService'),
    ('postgres', '', 'a_stock_fund_flow_daily', '东财 120 日日级个股资金流表，用于主力、小单、中单、大单、超大单资金趋势。', '["Stock Detail","Fund Flow","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Push2His fflow daykline'),
    ('postgres', '', 'a_stock_holder_num', '东财股东户数变化表，用于股东集中度和筹码变化跟踪。', '["Stock Detail","Fundamental Analysis","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Datacenter RPT_HOLDERNUMLATEST'),
    ('postgres', '', 'a_stock_hot_concept_snapshot', '东财个股热门概念命中快照，用于人气概念归因和题材热度解释。', '["Stock Detail","Concept Analysis","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney App hot concept'),
    ('postgres', '', 'a_stock_hot_rank_snapshot', '东财/同花顺人气榜快照，用于市场关注度、热榜排序和热度变化追踪。', '["Market Pulse","Hot Rank","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily', 'EastMoney App hot rank / THS hot list'),
    ('postgres', '', 'a_stock_hot_reason_snapshot', '同花顺热点强势股题材归因快照，用于解释个股上涨主题和事件驱动。', '["Market Pulse","Theme Review","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily', 'THS hot reason'),
    ('postgres', '', 'a_stock_industry_rank_snapshot', '东财行业板块涨跌排名快照，保留东财口径并与 THS/TDX 行业数据区分 source。', '["Market Pulse","Industry Ranking","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily', 'EastMoney Push2 clist industry rank'),
    ('postgres', '', 'a_stock_investor_interaction', '互动易问答表，保存投资者问题、公司回复和答复时间，用于事件与基本面补充。', '["Stock Detail","Research Workspace","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'CNInfo IRM / 互动易'),
    ('postgres', '', 'a_stock_limit_pool_snapshot', '东财/同花顺涨停池、炸板池、跌停池等原始池子快照，用于涨停生态和短线情绪分析。', '["Market Pulse","Limit Emotion","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily', 'EastMoney push2ex / THS limit_up_pool'),
    ('postgres', '', 'a_stock_lockup_expiry', '东财限售解禁表，记录历史和未来解禁日期、股份数量和占比。', '["Stock Detail","Risk Calendar","A Stock Skill"]'::jsonb, 'a_stock_code_daily', 'EastMoney Datacenter RPT_LIFT_STAGE'),
    ('postgres', '', 'a_stock_news', '东财个股新闻和 7x24 快讯表，用于资讯流、个股新闻摘要和市场事件输入。', '["News Feed","Stock Detail","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily / a_stock_code_daily', 'EastMoney Search / EastMoney 7x24'),
    ('postgres', '', 'a_stock_option_contract_snapshot', '新浪 ETF 期权合约快照，保存标的、月份、认购认沽和合约代码。', '["Option Monitor","A Stock Skill"]'::jsonb, 'a_stock_marketwide_daily', 'Sina ETF option API'),
    ('postgres', '', 'a_stock_research_report', '东财研报、行业研报、iwencai 语义研报和同花顺 EPS 预测统一表。', '["Research Workspace","Stock Detail","A Stock Skill"]'::jsonb, 'a_stock_research_weekly', 'EastMoney ReportAPI / iwencai / THS basic'),
    ('postgres', '', 'margin_account_stats', '两融账户统计表，记录融资融券投资者账户数量等市场杠杆结构指标。', '["Margin Trading","Market Pulse"]'::jsonb, 'margin_sync_daily', 'Exchange margin / AkShare fallback'),
    ('postgres', '', 'margin_market_summary', '沪深京市场两融汇总表，用于展示融资余额、融券余额、买入偿还等交易所口径汇总。', '["Margin Trading","Market Pulse"]'::jsonb, 'margin_sync_daily', 'Exchange margin / AkShare fallback'),
    ('postgres', '', 'margin_stock_detail', '个股两融明细表，用于个股融资余额、融资买入、融券卖出等排行和筛选。', '["Margin Trading","Stock Detail"]'::jsonb, 'margin_sync_daily', 'Exchange margin / AkShare fallback')
ON CONFLICT (source_type, schema_name, table_name) DO UPDATE SET
    business_logic = EXCLUDED.business_logic,
    used_by = EXCLUDED.used_by,
    update_job = EXCLUDED.update_job,
    data_source = EXCLUDED.data_source,
    enabled = true,
    updated_at = now();
