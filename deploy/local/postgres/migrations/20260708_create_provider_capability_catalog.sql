\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_core;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.provider_capability_catalog (
    id bigint NOT NULL PRIMARY KEY,
    capability_code varchar(128) NOT NULL,
    name varchar(256) NOT NULL,
    category varchar(64) NOT NULL,
    provider_code varchar(64) NOT NULL,
    provider_name varchar(128) NOT NULL,
    capability_type varchar(64) NOT NULL,
    source_layer varchar(64) NOT NULL DEFAULT 'cynexus-data',
    target_store varchar(64) NOT NULL DEFAULT 'mixed',
    status varchar(32) NOT NULL DEFAULT 'active',
    data_granularity varchar(64),
    storage_tables jsonb NOT NULL DEFAULT '[]'::jsonb,
    update_jobs jsonb NOT NULL DEFAULT '[]'::jsonb,
    upstream_endpoints jsonb NOT NULL DEFAULT '[]'::jsonb,
    used_by jsonb NOT NULL DEFAULT '[]'::jsonb,
    rate_limit_policy text,
    freshness_policy text,
    anti_scraping_notes text,
    description text,
    params jsonb NOT NULL DEFAULT '{}'::jsonb,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    sort_order integer NOT NULL DEFAULT 0,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_provider_capability_catalog_code_alive
    ON cynexus_appl_core.provider_capability_catalog (capability_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_provider_capability_catalog_provider_category
    ON cynexus_appl_core.provider_capability_catalog (provider_code, category, sort_order)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_core.provider_capability_catalog IS
    'Cynexus data provider capability catalog shown in Settings / Provider.';

WITH defs AS (
    SELECT *
      FROM (
        VALUES
        ('storage_postgres_core','PostgreSQL Core 元数据','storage','postgres','PostgreSQL','storage','cynexus-data','postgres','active','control-plane','data_source_health_check_run,data_source_health_check_item,scheduler_jobs,task_job_run','data_source_health_check,all scheduler jobs','','Settings,Job Executor','本地连接，无外部限流','实时查询','无','Core 控制面存储，承载调度、健康检查、任务运行和 Provider 能力目录。','{}'::jsonb,10),
        ('storage_postgres_market','PostgreSQL Market 业务快照','storage','postgres','PostgreSQL','storage','cynexus-data','postgres','active','snapshot/daily','sec_stock_basic,sec_stock_sector,mkt_overview_daily,market_limit_daily_snapshots,a_stock_research_report','market jobs,a_stock jobs,margin_sync_daily','','Market UI,Stock UI,Settings','本地连接，无外部限流','按表业务日期判断','无','Market 服务面向 UI 的关系型快照存储。','{}'::jsonb,11),
        ('storage_clickhouse','ClickHouse 行情分析存储','storage','clickhouse','ClickHouse','storage','cynexus-data','clickhouse','active','tick/minute/daily','daily_raw,daily_qfq,daily_hfq,index_daily_raw,ch_dwd_kline_1m,ch_ods_stock_fund_flow_minute,ch_ods_stock_transaction,ch_ods_stock_order_book_snapshot','sync_kline_daily,sync_adjusted_kline_daily,sync_kline_minute,ingest_tdx_1m_to_clickhouse,sync_a_stock_dataset','','Charts,Market Pulse,MSI,Intraday','本地连接，无外部限流','按业务日期和导入时间判断','无','高频与分析型行情事实表存储，承载 1mK、日 K、资金流、盘口和逐笔。','{}'::jsonb,12),
        ('storage_duckdb','DuckDB 本地分析文件','storage','duckdb','DuckDB','storage','cynexus-data','duckdb','active','offline','local duckdb file','market offline jobs','','Offline Research','本地文件','按文件存在性/更新时间判断','无','本地离线分析和中间数据文件。','{}'::jsonb,13),
        ('calendar_trade','交易日历','metadata','tdx_akshare','TDX / AkShare','calendar','cynexus-data','postgres','active','daily','sec_trade_calendar','sync_trade_calendar','','Schedulers,All market jobs','低频日历更新','年度/按需更新','无','交易日历和工作日判断，供所有调度和行情日期推导使用。','{}'::jsonb,20),
        ('stock_basic','股票基础信息','metadata','akshare_eastmoney','AkShare / EastMoney','security-master','cynexus-data','postgres','active','snapshot','sec_stock_basic,sec_stock_universe','sync_stock_basic,sync_metadata','AkShare stock info,EastMoney Push2 stock get','Search,Stock Overview,Kline jobs','盘后/低频，失败可 fallback','日级或按需','东财接口需常规 UA','股票代码、名称、市场、上市状态、行业、市值等主数据。','{}'::jsonb,21),
        ('stock_sector_topics','股票行业/题材/风格归属','metadata','eltdx_ths_eastmoney','eltdx / THS / EastMoney','security-sector','cynexus-data','postgres','active','snapshot','sec_sector,sec_stock_sector,tdx_ths_industry_mapping,a_stock_concept_membership','sync_stock_sectors,sync_ths_industry_constituents,a_stock_code_daily','eltdx F10 topics,THS constituents,EastMoney slist','Industry / Concept,Heatmap,Stock Detail','eltdx 批量需并发控制，THS 需 page_sleep','日级','THS 需 hexin-v，东财需限速','股票到行业、概念、风格板块的映射能力。','{}'::jsonb,22),
        ('stock_turnover','个股换手率序列','market-data','akshare_eastmoney_eltdx','AkShare / EastMoney / eltdx','factor-input','cynexus-data','postgres','active','daily','stock_turnover_series_snapshot','sync_stock_turnover','','Stock Overview,Market Sentiment','盘后批量，建议控制 pool_size','日级','批量抓取需 sleep/retry','个股换手率序列和流动性基础输入。','{}'::jsonb,23),
        ('kline_daily_raw','未复权日 K','kline','tdx_eastmoney_tencent','TDX / EastMoney / Tencent','kline','cynexus-data','clickhouse','active','1d','daily_raw,index_daily_raw','sync_kline_daily,sync_tdx_hsjday','TDX local day,EastMoney kline,Tencent quote','Stock Chart,Market Sentiment,MSI','盘后优先本地 TDX，网络 fallback 限速','日级','东财/Tencent fallback 需限速','未复权股票/指数日 K 基础事实。','{}'::jsonb,30),
        ('kline_daily_qfq','前复权日 K','kline','eltdx','eltdx / TDX','kline','cynexus-data','clickhouse','active','1d qfq','daily_qfq,adj_factors','sync_adjusted_kline_daily','eltdx adjusted kline','Stock Chart,MSI technical factors','盘后批量，按代码限并发','日级','TCP 源需 timeout/retry','前复权日 K，用于技术指标、涨跌幅和趋势广度计算。','{}'::jsonb,31),
        ('kline_daily_hfq','后复权日 K','kline','eltdx','eltdx / TDX','kline','cynexus-data','clickhouse','active','1d hfq','daily_hfq,adj_factors','sync_adjusted_kline_daily','eltdx adjusted kline','Long-term Review,Stock Chart','盘后批量，按代码限并发','日级','TCP 源需 timeout/retry','后复权日 K，用于长期收益和历史复盘。','{}'::jsonb,32),
        ('kline_minute_realtime','盘中实时分钟 K','kline','tencent_mootdx_sina_eastmoney','Tencent / mootdx / Sina / EastMoney','kline','cynexus-data','clickhouse','active','1m','intraday_bars,quotes,ch_dwd_kline_1m','sync_kline_minute','Tencent qt,mootdx,Sina minute,EastMoney minute','Stock Chart,Market Pulse','盘中 5 分钟级拉取，按 provider fallback','分钟级','实时 HTTP/TCP 源需短超时和 fallback','盘中实时 1m K 数据接口，覆盖指数、板块、ETF、个股的分钟行情入口。','{}'::jsonb,33),
        ('kline_tdx_local_1m','本地 TDX 1mK 解析','kline','tdx_local','Local TongDaXin','kline-ingest','cynexus-data','clickhouse','active','1m','ch_dwd_kline_1m,ingest_state','ingest_tdx_1m_to_clickhouse','local TDX vipdoc minline files','Stock Chart,Intraday Analytics','盘后本地文件解析，无外部限流','日级导入','需要通达信客户端下载本地数据','盘后解析本地通达信 1 分钟文件并写入 ClickHouse。','{}'::jsonb,34),
        ('kline_local_parquet_1m','本地 Parquet 1mK 读取','kline','local_parquet','Local Parquet','kline-ingest','cynexus-data','clickhouse','active','1m','ch_dwd_kline_1m','sync_kline_minute','local *.parquet 1m files','Stock Chart,Offline Research','本地文件，无外部限流','按文件更新时间/导入日期','无','读取标准 1 分钟 Parquet 文件，校验列并补齐来源文件元数据。','{}'::jsonb,34),
        ('kline_aggregate_5m','1m 聚合 5m/多周期 K','kline','internal','Internal','kline-transform','cynexus-data','clickhouse','active','5m+','ch_dwd_kline_1m,intraday_bars','aggregate_kline_periods','','Stock Chart','本地 ClickHouse 聚合','跟随 1mK','无','基于 1m K 聚合 5m 等更高周期行情。','{}'::jsonb,35),
        ('tdx_hsjday_download','TDX hsjday 下载与校验','kline','tdx','TongDaXin','file-download','cynexus-data','local-files','active','daily file','local TDX vipdoc day files','sync_tdx_hsjday','https://data.tdx.com.cn/vipdoc/hsjday.zip','Kline pipelines','盘后低频，支持重试和浏览器 fallback','日级','可能遇到 JS challenge 或 SSL EOF','下载、校验、解压并原子替换本地 TDX 日线数据包。','{}'::jsonb,36),
        ('tdx_auction_timeshare','竞价与历史分时细节','intraday','eltdx','eltdx / TDX','intraday-detail','cynexus-data','clickhouse','active','auction/1m timeshare','ch_ods_stock_auction_point,ch_ods_stock_timeshare_1m','sync_tdx_intraday_detail','eltdx get_call_auction,get_history_minute','Application Analysis,Stock Chart','交易日批量，控制 timeout/batch_size','日级/分钟级','TCP 源需 retry','集合竞价点位和历史分时 1m 明细。','{}'::jsonb,37),
        ('eastmoney_stock_info','东财个股基本面扩展','a-stock-skill','eastmoney','EastMoney','fundamental','cynexus-data','postgres','active','snapshot','sec_stock_basic','a_stock_code_daily','EastMoney Push2 stock get','Stock Detail,A Stock Skill','自选股批量，默认 2s sleep','日级','Push2 需 UA/Referer','补齐名称、行业、市值、总股本、流通股、上市日期等东财字段。','{}'::jsonb,40),
        ('eastmoney_concepts','东财个股概念归属','a-stock-skill','eastmoney','EastMoney','concept','cynexus-data','postgres','active','snapshot','a_stock_concept_membership','a_stock_code_daily','EastMoney Push2 slist','Stock Detail,Concept Analysis','自选股批量，默认 2s sleep','日级','Push2 需限速','个股概念板块归属和领涨股字段。','{}'::jsonb,41),
        ('eastmoney_fund_flow_minute','东财个股分钟资金流','a-stock-skill','eastmoney','EastMoney','fund-flow','cynexus-data','clickhouse','active','minute','ch_ods_stock_fund_flow_minute','a_stock_fund_flow_intraday','EastMoney Push2 fflow kline','Intraday Fund Flow,Stock Detail','盘中每 15 分钟，自选股默认 5s sleep','分钟级','高频接口需控制间隔','主力、小单、中单、大单、超大单分钟级资金流。','{}'::jsonb,42),
        ('eastmoney_fund_flow_120d','东财 120 日日级资金流','a-stock-skill','eastmoney','EastMoney','fund-flow','cynexus-data','postgres','active','daily 120d','a_stock_fund_flow_daily','a_stock_code_daily','EastMoney Push2His fflow daykline','Stock Detail,Fund Flow','盘后自选股批量，默认 2s sleep','日级','Push2His 需限速','个股近 120 日主力及分单资金流。','{}'::jsonb,43),
        ('eastmoney_industry_rank','东财行业板块排名','a-stock-skill','eastmoney','EastMoney','industry-ranking','cynexus-data','postgres','active','snapshot','a_stock_industry_rank_snapshot','a_stock_marketwide_daily','EastMoney Push2 clist industry','Market Pulse,Industry Ranking','盘后低频，建议 2s sleep 和重试','日级快照','近期可能 RemoteDisconnected，需 fallback/retry','东财口径行业涨跌排名，和 THS/TDX 行业数据按 source 区分。','{}'::jsonb,44),
        ('eastmoney_dragon_tiger_market','东财全市场龙虎榜','a-stock-skill','eastmoney','EastMoney','dragon-tiger','cynexus-data','postgres','active','daily','a_stock_dragon_tiger_daily','a_stock_marketwide_daily','EastMoney Datacenter RPT_DAILYBILLBOARD_DETAILSNEW','Market Review,A Stock Skill','盘后低频','交易日','Datacenter 需限速','全市场龙虎榜上榜股票、原因、净买额、买卖额。','{}'::jsonb,45),
        ('eastmoney_dragon_tiger_seats','东财个股龙虎榜席位','a-stock-skill','eastmoney','EastMoney','dragon-tiger-seat','cynexus-data','postgres','active','daily','a_stock_dragon_tiger_daily,a_stock_dragon_tiger_seat','a_stock_code_daily','EastMoney Datacenter billboard buy/sell','Stock Detail,A Stock Skill','自选股低频，默认 2s sleep','交易日','Datacenter 需限速','个股龙虎榜历史和买卖席位明细。','{}'::jsonb,46),
        ('eastmoney_lockup','东财限售解禁','a-stock-skill','eastmoney','EastMoney','corporate-action','cynexus-data','postgres','active','calendar','a_stock_lockup_expiry','a_stock_code_daily','EastMoney Datacenter RPT_LIFT_STAGE','Risk Calendar,Stock Detail','盘后自选股批量','日级/日历','Datacenter 需限速','限售解禁历史和未来日历。','{}'::jsonb,47),
        ('eastmoney_block_trade','东财大宗交易','a-stock-skill','eastmoney','EastMoney','block-trade','cynexus-data','postgres','active','daily','a_stock_block_trade','a_stock_code_daily','EastMoney Datacenter RPT_DATA_BLOCKTRADE','Stock Detail,Risk Review','盘后自选股批量','日级','Datacenter 需限速','大宗交易折溢价、成交量、买卖方。','{}'::jsonb,48),
        ('eastmoney_holder_num','东财股东户数','a-stock-skill','eastmoney','EastMoney','holder','cynexus-data','postgres','active','periodic','a_stock_holder_num','a_stock_code_daily','EastMoney Datacenter RPT_HOLDERNUMLATEST','Fundamental Analysis,Stock Detail','低频批量','公告/报告期','Datacenter 需限速','股东户数、变化比例和户均流通股。','{}'::jsonb,49),
        ('eastmoney_dividend','东财分红送转','a-stock-skill','eastmoney','EastMoney','corporate-action','cynexus-data','postgres','active','event','a_stock_dividend','a_stock_code_daily','EastMoney Datacenter RPT_SHAREBONUS_DET','Corporate Actions,Stock Detail','低频批量','事件日','Datacenter 需限速','分红、送转、方案进度和除权日期。','{}'::jsonb,50),
        ('eastmoney_stock_news','东财个股新闻','a-stock-skill','eastmoney','EastMoney','news','cynexus-data','postgres','active','event','a_stock_news','a_stock_code_daily','EastMoney Search API','News Feed,Stock Detail','自选股批量，默认 2s sleep','日内/事件','搜索接口需 Referer','个股新闻搜索和摘要。','{}'::jsonb,51),
        ('eastmoney_global_news','东财全球 7x24 快讯','a-stock-skill','eastmoney','EastMoney','news','cynexus-data','postgres','active','realtime','a_stock_news','a_stock_marketwide_daily','EastMoney 7x24 fast news','News Feed,Market Pulse','盘后/可盘中低频','分钟/事件','快讯接口需限速','东财 7x24 全局资讯流。','{}'::jsonb,52),
        ('eastmoney_research_reports','东财个股/行业研报','a-stock-skill','eastmoney','EastMoney','research','cynexus-data','postgres','active','research','a_stock_research_report','a_stock_research_weekly','EastMoney ReportAPI','Research Workspace,Stock Detail','周频/低频，默认 2s sleep','周级/发布日','ReportAPI 需限速','东财个股研报、行业研报和 PDF 元数据。','{}'::jsonb,53),
        ('iwencai_research_search','iwencai 语义研报搜索','a-stock-skill','iwencai','iwencai','research','cynexus-data','postgres','active','research','a_stock_research_report','a_stock_research_weekly','iwencai query/search','Research Workspace','周频，默认 3s sleep','周级/发布日','需要 IWENCAI_API_KEY','语义化研报和投研文本搜索。','{}'::jsonb,54),
        ('ths_eps_forecast','同花顺 EPS 一致预期','a-stock-skill','ths','TongHuaShun','research','cynexus-data','postgres','active','forecast','a_stock_research_report','a_stock_research_weekly','basic.10jqka.com.cn worth','Fundamental Analysis,Research Workspace','周频/低频','周级','HTML 表格可能反爬','同花顺 basic 页面 EPS 预测和一致预期。','{}'::jsonb,55),
        ('eastmoney_limit_pool','东财涨跌停原始池','a-stock-skill','eastmoney','EastMoney','limit-pool','cynexus-data','postgres','active','snapshot','a_stock_limit_pool_snapshot','a_stock_marketwide_daily','EastMoney push2ex ZT/ZB/DT/YZT pool','Market Pulse,Limit Emotion','盘后低频，池子分批','日级快照','push2ex 需 Referer/限速','涨停池、炸板池、跌停池、昨日涨停池原始池子。','{}'::jsonb,56),
        ('ths_hot_reason','同花顺热点强势股归因','a-stock-skill','ths','TongHuaShun','theme-reason','cynexus-data','postgres','active','snapshot','a_stock_hot_reason_snapshot','a_stock_marketwide_daily','THS getharden','Theme Review,Market Pulse','盘后低频，默认 1s sleep','日级快照','THS 接口需控制频率','强势股题材归因和事件标签。','{}'::jsonb,57),
        ('ths_limit_up_pool','同花顺涨停池/涨停原因','a-stock-skill','ths','TongHuaShun','limit-pool','cynexus-data','postgres','active','snapshot','a_stock_limit_pool_snapshot','a_stock_marketwide_daily','THS limit_up_pool','Limit Emotion,Market Pulse','盘后低频，默认 1s sleep','日级快照','THS 可能需要 cookie/hexin-v','同花顺涨停池、涨停揭秘和涨停原因字段。','{}'::jsonb,58),
        ('eastmoney_hot_rank','东财人气榜','a-stock-skill','eastmoney','EastMoney','hot-rank','cynexus-data','postgres','active','snapshot','a_stock_hot_rank_snapshot','a_stock_marketwide_daily','EastMoney app hot rank','Hot Rank,Market Pulse','盘后/低频，默认 5s sleep','日内快照','App 接口易断连，需重试','东财个股人气榜和排名变化。','{}'::jsonb,59),
        ('ths_hot_list','同花顺热榜','a-stock-skill','ths','TongHuaShun','hot-rank','cynexus-data','postgres','active','snapshot','a_stock_hot_rank_snapshot','a_stock_marketwide_daily','THS hot_list','Hot Rank,Market Pulse','盘后/低频，默认 1s sleep','日内快照','THS 接口需限速','同花顺股票热榜、热度和概念标签。','{}'::jsonb,60),
        ('eastmoney_hot_concept','东财热门概念命中','a-stock-skill','eastmoney','EastMoney','concept-hot','cynexus-data','postgres','active','snapshot','a_stock_hot_concept_snapshot','a_stock_code_daily','EastMoney app hot stock rank list','Stock Detail,Concept Analysis','自选股批量，默认 5s sleep','日级快照','App 接口需限速','个股热门概念命中和命中次数。','{}'::jsonb,61),
        ('ths_northbound_minute','同花顺北向资金分钟流向','a-stock-skill','ths','TongHuaShun','fund-flow','cynexus-data','clickhouse','active','minute','ch_ods_northbound_minute','a_stock_fund_flow_intraday','THS hsgt dayChart','Market Pulse,Intraday Fund Flow','盘中每 15 分钟','分钟级','THS 接口需限速','沪股通/深股通北向资金分钟流向。','{}'::jsonb,62),
        ('sina_option_contracts','新浪 ETF 期权合约','a-stock-skill','sina','Sina Finance','option','cynexus-data','postgres','active','snapshot','a_stock_option_contract_snapshot','a_stock_marketwide_daily','Sina StockOptionService.getStockName','Option Monitor','盘后低频','日级快照','Sina hq 需 Referer','50ETF/300ETF/500ETF/科创 50ETF 期权合约代码。','{}'::jsonb,63),
        ('sina_option_quote_greeks','新浪期权 T 型报价/希腊字母/IV','a-stock-skill','sina','Sina Finance','option','cynexus-data','clickhouse','active','realtime','ch_ods_option_quote_snapshot','sync_a_stock_dataset','Sina hq CON_OP/CON_SO','Option Monitor','盘中低频，默认 0.5s sleep','分钟/快照','Sina hq 需 Referer','期权报价、成交、持仓、Delta/Gamma/Theta/Vega/IV。','{}'::jsonb,64),
        ('sina_financial_statement','新浪财务报表','a-stock-skill','sina','Sina Finance','financial-statement','cynexus-data','postgres','active','report-period','a_stock_financial_statement','a_stock_code_daily','Sina finance report API','Fundamental Analysis,Stock Detail','自选股低频，默认 0.5s sleep','报告期/公告更新','Sina 接口需限速','新浪利润表、资产负债表等财务报表摘要，作为个股基本面补充。','{}'::jsonb,64),
        ('baidu_kline_ma','百度股市通 K 线 MA','a-stock-skill','baidu','Baidu Stock','kline','cynexus-data','clickhouse','active','daily/minute','ch_ods_baidu_kline_ma','sync_a_stock_dataset','Baidu selfselect getstockquotation','Stock Chart,A Stock Skill','低频 fallback，默认 1s sleep','日级/请求窗口','Baidu 接口需 Origin/Referer','百度股市通 K 线，带 MA5/10/20 字段。','{}'::jsonb,65),
        ('mootdx_order_book','mootdx 五档盘口','a-stock-skill','mootdx','mootdx / TDX TCP','order-book','cynexus-data','clickhouse','active','realtime','ch_ods_stock_order_book_snapshot','a_stock_tdx_realtime_probe','mootdx quotes','Intraday Monitor,Stock Detail','盘中每 10 分钟，自选股 0.2s sleep','实时快照','TCP 源需 timeout/fallback server','五档买卖盘、价格、成交量和盘口快照。','{}'::jsonb,66),
        ('mootdx_transactions','mootdx 逐笔成交','a-stock-skill','mootdx','mootdx / TDX TCP','transaction','cynexus-data','clickhouse','active','tick/transaction','ch_ods_stock_transaction','a_stock_tdx_realtime_probe','mootdx transaction','Intraday Monitor,Stock Detail','盘中每 10 分钟，自选股 0.2s sleep','实时/交易日','TCP 源需 timeout/fallback server','逐笔成交时间、价格、量、金额和买卖方向。','{}'::jsonb,67),
        ('cninfo_announcements','巨潮公告全文检索/下载','a-stock-skill','cninfo','CNInfo / 巨潮','announcement','cynexus-data','postgres','active','event','a_stock_announcement','a_stock_code_daily','CNInfo hisAnnouncement query/static pdf','Research Workspace,Stock Detail','自选股低频，默认 1.5s sleep','日级/事件','CNInfo 需 Referer，全文下载需静态链接','公告检索、公告摘要和 PDF 下载链接元数据。','{}'::jsonb,68),
        ('cninfo_irm','互动易问答','a-stock-skill','cninfo','CNInfo / 互动易','interaction','cynexus-data','postgres','active','event','a_stock_investor_interaction','a_stock_code_daily','CNInfo IRM question API','Research Workspace,Stock Detail','自选股低频，默认 1.5s sleep','日级/事件','互动易接口需限速','投资者问答、公司回复和答复人信息。','{}'::jsonb,69),
        ('ths_sector_fund_flow','同花顺行业资金流','sector','ths','TongHuaShun','fund-flow','cynexus-data','postgres','active','daily','sector_fund_flow_daily_snapshots,sector_fund_flow_capture_batches,mkt_ths_industry_fund_flow_daily','sync_fund_flow','THS funds hyzjl','Market Pulse,Industry / Concept','盘后 page_sleep 默认 0.4s','日级','需要 hexin-v token','同花顺行业资金净流入、涨跌幅和排名。','{}'::jsonb,70),
        ('tdx_industry_quotes','TDX 56 行业行情','sector','eltdx','eltdx / TDX','industry-quote','cynexus-data','postgres','active','snapshot','tdx_industry_quote_snapshot,tdx_industry_quote_batch','sync_tdx_industry_quotes','eltdx get_index_codes_all + get_quote','Heatmap,Industry / Concept','盘后/TCP 批量，控制 pool_size','日级快照','TCP 源需 timeout','TDX 56 行业指数行情快照。','{}'::jsonb,71),
        ('tdx_industry_fund_flow','TDX 56 行业资金','sector','eltdx','eltdx / TDX','fund-flow','cynexus-data','postgres','active','daily','tdx_industry_fund_flow_snapshot,tdx_industry_fund_flow_batch','sync_tdx_industry_fund_flow','eltdx industry fund flow','Industry / Concept','盘后/TCP 批量','日级','TCP 源需 timeout','TDX 行业资金快照。','{}'::jsonb,72),
        ('tdx_sector_quote_detail','TDX 板块详情','sector','eltdx','eltdx / TDX','sector-detail','cynexus-data','postgres','active','snapshot','tdx_sector_quote_detail_snapshot','sync_tdx_sector_quote_details','eltdx f10.theme_market','Industry / Concept','盘后/TCP 批量','日级快照','TCP 源需 timeout','TDX 板块资金、行情和成分详情 JSON。','{}'::jsonb,73),
        ('market_overview','市场概览','market-app','akshare_eltdx_tencent_eastmoney','AkShare / eltdx / Tencent / EastMoney','market-overview','cynexus-data','postgres','active','daily/snapshot','market_overview_snapshots,mkt_overview_daily','build_market_overview','AkShare market overview,eltdx quotes,Tencent,EastMoney','Dashboard,Market Pulse,MSI','盘后/可手动，provider fallback','日级','外部源需 fallback','市场涨跌家数、成交额、主力净流等概览。','{}'::jsonb,80),
        ('market_overview_detail','市场技术详情快照','market-app','internal','Internal','market-detail','cynexus-data','postgres','active','daily','mkt_overview_detail_snapshot','build_market_overview_detail','','Market Pulse','本地 CH/PG 计算','日级','无','基于指数 K 线、广度和行业数据生成技术详情 JSON。','{}'::jsonb,81),
        ('market_heatmap','市场热力图','market-app','internal_tdx','Internal / TDX','heatmap','cynexus-data','postgres','active','snapshot','mkt_heatmap_snapshot','build_market_heatmap','tdx_industry_quote_snapshot','Heatmap,Market Pulse','本地 PG 计算','日级快照','无','行业热力图 UI JSON 快照。','{}'::jsonb,82),
        ('market_pulse','Market Pulse 行业脉搏','market-app','internal','Internal','market-pulse','cynexus-data','postgres','active','daily','market_pulse_capture_batches,market_pulse_sector_daily_snapshots,mkt_sector_pulse_daily','build_market_pulse','sector_fund_flow_daily_snapshots','Market Pulse','本地 PG 计算','日级','无','市场脉搏行业热度、资金和强弱排序。','{}'::jsonb,83),
        ('style_sector','风格板块快照','market-app','tencent_internal','Tencent / Internal','style-sector','cynexus-data','postgres','active','daily','mkt_style_sector_snapshot','build_style_sector','sec_sector + Tencent realtime quotes','Market Sentiment,Market Pulse','盘后/批量 Tencent quote','日级','Tencent 批量需 sleep','风格板块等权涨跌和轮动输入。','{}'::jsonb,84),
        ('index_returns','宽基指数收益窗口','msi','clickhouse_internal','ClickHouse / Internal','factor-input','cynexus-data','postgres','active','daily','mkt_index_return_daily','build_index_returns','ClickHouse index_daily_raw','Market Sentiment,MSI','本地 CH 读取','日级','无','沪深主要指数收益窗口。','{}'::jsonb,90),
        ('msi_ma_count','均线占比因子','msi','clickhouse_internal','ClickHouse / Internal','msi-factor','cynexus-data','postgres','active','daily','msi_ma_count_daily','build_ma_count','ClickHouse daily_qfq','Market Sentiment','本地 CH 计算','日级','无','市场均线占比、5 日上涨比例和新低比例。','{}'::jsonb,91),
        ('msi_limit_emotion','涨跌停情绪因子','msi','internal','Internal','msi-factor','cynexus-data','postgres','active','daily','msi_limit_emotion_daily,market_limit_daily_snapshots,market_limit_daily_stocks','build_limit_emotion','daily_qfq / limit snapshots','Market Sentiment,Market Pulse','本地计算','日级','无','涨停、跌停、连板高度和炸板率情绪因子。','{}'::jsonb,92),
        ('msi_profit_effect','赚钱效应因子','msi','internal','Internal','msi-factor','cynexus-data','postgres','active','daily','msi_profit_effect_daily','build_profit_effect','msi_ma_count_daily','Market Sentiment','本地计算','日级','无','基于上涨比例和新低比例计算赚钱效应。','{}'::jsonb,93),
        ('msi_turnover_activity','成交活跃度因子','msi','clickhouse_internal','ClickHouse / Internal','msi-factor','cynexus-data','postgres','active','daily','msi_turnover_activity_daily','build_turnover_activity','ClickHouse daily/index bars','Market Sentiment','本地 CH 计算','日级','无','基于市场成交额窗口计算成交活跃度。','{}'::jsonb,94),
        ('msi_volatility','波动情绪因子','msi','clickhouse_internal','ClickHouse / Internal','msi-factor','cynexus-data','postgres','active','daily','msi_volatility_daily','build_volatility_sentiment','ClickHouse index close series','Market Sentiment','本地 CH 计算','日级','无','基于指数波动率计算波动情绪。','{}'::jsonb,95),
        ('msi_risk_appetite','风险偏好因子','msi','clickhouse_internal','ClickHouse / Internal','msi-factor','cynexus-data','postgres','active','daily','msi_risk_appetite_daily','build_risk_appetite','HS300 + treasury ETF returns','Market Sentiment','本地 CH 计算','日级','无','HS300 与国债 ETF 相对表现的风险偏好指标。','{}'::jsonb,96),
        ('msi_style_risk','风格风险因子','msi','clickhouse_internal','ClickHouse / Internal','msi-factor','cynexus-data','postgres','active','daily','msi_style_risk_daily','build_style_risk','CSI1000 vs HS300 returns','Market Sentiment','本地 CH 计算','日级','无','小盘/大盘风格相对风险偏好。','{}'::jsonb,97),
        ('msi_sector_breadth','行业广度因子','msi','internal','Internal','msi-factor','cynexus-data','postgres','active','daily','msi_sector_breadth_daily','build_sector_breadth','sector fund flow snapshots','Market Sentiment','本地 PG 计算','日级','无','行业上涨/下跌广度指标。','{}'::jsonb,98),
        ('msi_composite','市场情绪指数 MSI','msi','internal','Internal','composite-index','cynexus-data','postgres','active','daily','msi_index_daily','build_msi,rebuild_msi,market_sentiment_chain_refresh','MSI factor tables','Market Sentiment','本地聚合','日级','无','综合市场情绪指数和子因子汇总。','{}'::jsonb,99),
        ('margin_trading','两融数据','margin','exchange_akshare','Exchange / AkShare','margin','cynexus-data','postgres','active','daily','margin_market_summary,margin_stock_detail,margin_account_stats','margin_sync_daily,sync_margin','Exchange margin endpoints,AkShare margin','Margin Trading,Market Pulse','盘后交易所披露后抓取，AkShare fallback','交易日','交易所披露有滞后','两融汇总、个股明细和账户统计。','{}'::jsonb,110),
        ('tencent_stock_fund_flow','腾讯个股资金流快照','fund-flow','tencent','Tencent','fund-flow','cynexus-data','postgres','active','snapshot','stock_fund_flow_snapshot,stock_fund_flow_capture_batch','sync_stock_fund_flow','Tencent qt fund flow','Stock Overview','批量低频','日级/快照','Tencent 批量需 sleep','腾讯个股资金流快照。','{}'::jsonb,111),
        ('media_transcription','媒体转写','media','local_whisper','Local / Transformers Whisper','transcription','cynexus-data','postgres/local','active','task','media_transcript,task_job_run','transcribe_media','local file/remote download','MP4 to Word','本地任务队列','任务级','远程下载需超时和大小控制','MP4/音频下载、转音频和本地转写。','{}'::jsonb,120),
        ('data_quality_health','数据源健康检查','ops','internal','Internal','quality','cynexus-data','postgres','active','snapshot','data_source_health_check_run,data_source_health_check_item','data_source_health,health_check,quality_check','provider probes','Settings Data Source,Settings Provider','手动/低频，不跑慢探针时跳过 TCP 重型检查','按运行时间','外部 HTTP 探针需短超时','数据源连通性、真实性、耗时和成功率检查。','{}'::jsonb,130),
        ('ops_trigger_probe','Job Executor 触发探针','ops','internal','Internal','runtime-probe','cynexus-data','postgres','active','request','scheduler_job_run_history','trigger_probe','data-job-executor internal API','Scheduler Smoke Test','本地服务调用','按触发时间','无','Java/Core 到 Python data-job-executor 的端到端触发探针，用于联通性验证。','{}'::jsonb,131),
        ('ops_backfill_history','历史数据回填入口','ops','internal','Internal','backfill','cynexus-data','mixed','active','date-range','daily_raw,daily_qfq,daily_hfq,market snapshots','backfill_history','configured providers by dataset','Data Repair,Operations','按 dataset provider 策略执行','按回填范围','外部源回填需限速和断点续跑','通用历史数据回填 Job 入口，按数据集和日期范围调用对应 provider。','{}'::jsonb,132),
        ('ops_repair_missing_data','缺失数据修复入口','ops','internal','Internal','repair','cynexus-data','mixed','active','dataset/date','target dataset tables','repair_missing_data','configured providers by dataset','Data Repair,Operations','按 dataset provider 策略执行','按修复目标','外部源修复需限速和重试','通用缺失数据修复 Job 入口，用于按数据集和交易日补洞。','{}'::jsonb,133),
        ('ops_provider_fallback','Provider fallback 编排','ops','internal','Internal','source-orchestration','cynexus-data','mixed','active','request','provider_fallback runtime','sync_kline_daily,sync_kline_minute,sync_metadata','sources.yaml provider plans','All source-backed pipelines','本地编排，无外部限流；实际限流由下游 provider 决定','请求级','按 provider 配置短超时和 fallback','根据 sources.yaml 为日 K、分钟 K、资金流、基础信息等选择 provider 顺序并记录失败原因。','{}'::jsonb,134)
      ) AS t(capability_code,name,category,provider_code,provider_name,capability_type,source_layer,target_store,status,data_granularity,storage_tables,update_jobs,upstream_endpoints,used_by,rate_limit_policy,freshness_policy,anti_scraping_notes,description,params,sort_order)
),
numbered AS (
    SELECT row_number() OVER (ORDER BY sort_order, capability_code) AS rn, *
      FROM defs
)
INSERT INTO cynexus_appl_core.provider_capability_catalog (
    id, capability_code, name, category, provider_code, provider_name, capability_type,
    source_layer, target_store, status, data_granularity, storage_tables, update_jobs,
    upstream_endpoints, used_by, rate_limit_policy, freshness_policy, anti_scraping_notes,
    description, params, extra, sort_order, remark, created_at, updated_at
)
SELECT
    910112000000100000 + rn,
    capability_code, name, category, provider_code, provider_name, capability_type,
    source_layer, target_store, status, data_granularity,
    CASE WHEN storage_tables = '' THEN '[]'::jsonb ELSE to_jsonb(string_to_array(storage_tables, ',')) END,
    CASE WHEN update_jobs = '' THEN '[]'::jsonb ELSE to_jsonb(string_to_array(update_jobs, ',')) END,
    CASE WHEN upstream_endpoints = '' THEN '[]'::jsonb ELSE to_jsonb(string_to_array(upstream_endpoints, ',')) END,
    CASE WHEN used_by = '' THEN '[]'::jsonb ELSE to_jsonb(string_to_array(used_by, ',')) END,
    rate_limit_policy, freshness_policy, anti_scraping_notes, description,
    params, '{}'::jsonb, sort_order, 'migration:20260708_create_provider_capability_catalog', now(), now()
FROM numbered
ON CONFLICT (capability_code) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    provider_code = EXCLUDED.provider_code,
    provider_name = EXCLUDED.provider_name,
    capability_type = EXCLUDED.capability_type,
    source_layer = EXCLUDED.source_layer,
    target_store = EXCLUDED.target_store,
    status = EXCLUDED.status,
    data_granularity = EXCLUDED.data_granularity,
    storage_tables = EXCLUDED.storage_tables,
    update_jobs = EXCLUDED.update_jobs,
    upstream_endpoints = EXCLUDED.upstream_endpoints,
    used_by = EXCLUDED.used_by,
    rate_limit_policy = EXCLUDED.rate_limit_policy,
    freshness_policy = EXCLUDED.freshness_policy,
    anti_scraping_notes = EXCLUDED.anti_scraping_notes,
    description = EXCLUDED.description,
    params = EXCLUDED.params,
    sort_order = EXCLUDED.sort_order,
    remark = EXCLUDED.remark,
    updated_at = now();
