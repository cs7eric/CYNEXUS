# Cynexus Data A 股能力全量地图

本文档汇总 `cynexus-data` 目前已有能力、2026-07-06/2026-07-07 新接入能力、落库目标和后续使用注意事项。后续做 UI、策略、AI 分析、知识检索时，以本文档作为能力索引。

## 核心数据流

`source provider -> domain normalization -> pipeline orchestration -> storage -> job executor/internal API`

- Source：外部接口、文件、TCP 行情协议适配。
- Domain：纯计算、字段归一化、口径转换。
- Pipeline：组合 source/domain/storage，处理 dry-run、限速、批量和参数。
- Storage：PostgreSQL/ClickHouse 写入和查询。
- Job：Core scheduler 和 trigger API 调用的执行入口。
- Internal API：`/internal/*` 服务间查询面，不直接替代 pipeline 落库。

## 已有核心能力

### K 线与行情

| 能力 | 来源 | 存储 | 说明 |
| --- | --- | --- | --- |
| 日 K 原始 | TDX local day/hsjday, eltdx, fallback providers | ClickHouse `daily_raw` | 主历史行情事实表 |
| 前复权/后复权日 K | eltdx + 复权流水线 | ClickHouse `daily_qfq`, `daily_hfq` | MSI、技术分析、股票详情使用 |
| 1 分钟 K | TDX local/Parquet ingest | ClickHouse `ch_dwd_kline_1m` | 股票图、应用分析详情页使用 |
| 周期聚合 | `aggregate_kline_periods` | ClickHouse | 从 1m 聚合 5m/15m/30m 等 |
| 指数日 K | migrated DuckDB/TDX | ClickHouse `index_daily_raw` | 大盘、MSI、指数收益窗口使用 |
| eltdx 分时/竞价 | eltdx source + intraday pipeline | ClickHouse `ch_ods_stock_auction_point`, `ch_ods_stock_timeshare_1m` | 可部分替代五档/逐笔的细节视图 |

### 股票基础信息

| 能力 | 来源 | 存储 | 说明 |
| --- | --- | --- | --- |
| 股票基础列表 | AkShare/eltdx/EastMoney fallback | PostgreSQL `sec_stock_basic` | 名称、市场、板块、ST、行业、市值等 |
| 股票行业/概念映射 | eltdx topics | PostgreSQL stock sector tables | 支撑行业热力图、股票详情 |
| THS 行业成分 | THS + 历史 fallback | PostgreSQL `ths_industry_constituent_snapshot` | 90 个 THS 行业代码和成分股 |
| 股票换手率序列 | ClickHouse 日 K + eltdx 股本 | PostgreSQL stock turnover tables | UI 轻量查询 |

### 市场脉搏与 MSI

| 能力 | 来源 | 存储 | 说明 |
| --- | --- | --- | --- |
| 市场概览 | AkShare/EastMoney/eltdx/ClickHouse fallback | PostgreSQL `market_overview_snapshots`, `mkt_overview_daily` | 成交额、涨跌家数、资金概况 |
| 市场详情 JSON | ClickHouse + domain 技术指标 | PostgreSQL market overview detail | UI 详情页使用 |
| 涨停情绪 | ClickHouse daily_raw + domain | PostgreSQL `market_limit_daily_*`, `msi_limit_emotion_daily` | MSI 因子和涨停情绪面板 |
| 行业/风格热力图 | TDX/THS/Tencent | PostgreSQL heatmap/style tables | UI heatmap 使用 |
| MSI 因子 | ClickHouse/PG factor tables | PostgreSQL `msi_*` | 已修复历史百分位语义 |
| 行业资金流 | THS/TDX | PostgreSQL sector flow / TDX industry flow tables | THS/TDX 口径并存 |

### 资金流与融资融券

| 能力 | 来源 | 存储 | 说明 |
| --- | --- | --- | --- |
| 板块资金流 | THS/TDX | PostgreSQL | 行业/板块流向 |
| 个股资金流快照 | Tencent | PostgreSQL `stock_fund_flow_snapshot` | 目前为空时股票详情使用 TDX 日 K fallback |
| 融资融券 | exchange/akshare | PostgreSQL margin tables | 已有 pipeline，不新增东财平行主表 |

### F10 与资讯

| 能力 | 来源 | 存储/接口 | 说明 |
| --- | --- | --- | --- |
| F10 估值、财务、业务、盈利预测 | eltdx F10 | `cynexus-data` internal F10 routes -> `cynexus-market` | 股票详情弹窗使用 |
| F10 公告、新闻、路演、公司新闻 | eltdx F10 | internal query routes | 摘要级能力 |

### 调度和执行

| 能力 | 位置 | 说明 |
| --- | --- | --- |
| Python job executor | `cynexus-data` trigger API | Core scheduler 调用 |
| Job state/checkpoint/lock | PostgreSQL core/data executor tables | 支持中断恢复和进度 |
| Chain progress | `market_sentiment_chain_refresh` | 14 步 MSI 链路可回传进度 |

## 2026-07-06 新增 Source/Query 能力

### EastMoney

| Dataset | Internal route | 今日状态 | 备注 |
| --- | --- | --- | --- |
| `eastmoney_reports` | `/internal/a-stock/eastmoney/reports` | 已接入 | 个股研报 |
| `eastmoney_industry_reports` | `/internal/a-stock/eastmoney/industry-reports` | 已接入 | 行业研报 |
| `eastmoney_stock_info` | `/internal/a-stock/eastmoney/stock-info` | 已接入 | 补总股本、流通股、上市日期 |
| `eastmoney_concept_blocks` | `/internal/a-stock/eastmoney/concept-blocks` | 已接入 | slist 个股概念归属 |
| `eastmoney_fund_flow_minute` | `/internal/a-stock/eastmoney/fund-flow-minute` | 已接入 | 分钟级资金流，smoke 中偶发 reset |
| `eastmoney_fund_flow_120d` | `/internal/a-stock/eastmoney/fund-flow-120d` | 已接入 | 120 日资金流，smoke 中 reset |
| `market_fund_flow_daily` | `sync_market_fund_flow` | 已接入 | 东方财富大盘日级主力/超大/大/中/小单资金流，交易日每日 17:45，归属 `data_collection` |
| `eastmoney_industry_rank` | `/internal/a-stock/eastmoney/industry-rank` | 已接入 | 东财行业排名，smoke 中 reset |
| `eastmoney_daily_dragon_tiger` | `/internal/a-stock/eastmoney/daily-dragon-tiger` | 已接入 | 全市场龙虎榜 |
| `eastmoney_dragon_tiger` | `/internal/a-stock/eastmoney/dragon-tiger` | 已接入 | 单股龙虎榜与席位 |
| `eastmoney_lockup` | `/internal/a-stock/eastmoney/lockup` | 已接入 | 限售解禁 |
| `eastmoney_margin` | `/internal/a-stock/eastmoney/margin` | 已接入查询 | 持久化复用已有 margin 口径，不新增主表 |
| `eastmoney_block_trade` | `/internal/a-stock/eastmoney/block-trade` | 已接入 | 大宗交易 |
| `eastmoney_holders` | `/internal/a-stock/eastmoney/holders` | 已接入 | 股东户数 |
| `eastmoney_dividends` | `/internal/a-stock/eastmoney/dividends` | 已接入 | 分红送转 |
| `eastmoney_stock_news` | `/internal/a-stock/eastmoney/stock-news` | 已接入 | 个股新闻 |
| `eastmoney_global_news` | `/internal/a-stock/eastmoney/global-news` | 已接入 | 全球 7x24 |
| `eastmoney_limit_pool` | `/internal/a-stock/eastmoney/limit-pool` | 已接入 | 涨停/炸板/跌停/昨日涨停池 |
| `eastmoney_limit_sentiment` | `/internal/a-stock/eastmoney/limit-sentiment` | 已接入查询 | 由 limit pools 计算 |
| `eastmoney_hot_rank` | `/internal/a-stock/eastmoney/hot-rank` | 已接入 | smoke 中 emappdata reset |
| `eastmoney_hot_concept` | `/internal/a-stock/eastmoney/hot-concept` | 已接入 | 热门概念命中 |

### THS

| Dataset | Internal route | 今日状态 | 备注 |
| --- | --- | --- | --- |
| `ths_eps_forecast` | `/internal/a-stock/ths/eps-forecast` | 已接入 | 同花顺 EPS 一致预期 |
| `ths_hot_reason` | `/internal/a-stock/ths/hot-reason` | 已接入 | 热点题材归因 |
| `ths_northbound` | `/internal/a-stock/ths/northbound` | 已接入 | 北向分钟流向 |
| `ths_limit_up_pool` | `/internal/a-stock/ths/limit-up-pool` | 已接入 | 同花顺涨停池 |
| `ths_hot_list` | `/internal/a-stock/ths/hot-list` | 已接入 | 同花顺热榜 |

### CNInfo

| Dataset | Internal route | 今日状态 | 备注 |
| --- | --- | --- | --- |
| `cninfo_announcements` | `/internal/a-stock/cninfo/announcements` | 已接入 | 巨潮公告检索，含 PDF URL |
| `cninfo_irm` | `/internal/a-stock/cninfo/irm` | 已接入 | 投资者问答 |

### Sina

| Dataset | Internal route | 今日状态 | 备注 |
| --- | --- | --- | --- |
| `sina_financial_report` | `/internal/a-stock/sina/financial-report` | 已接入 | 财务报表 |
| `sina_option_codes` | `/internal/a-stock/sina/options/codes` | 已接入 | ETF 期权合约 |
| `sina_option_tquote` | `/internal/a-stock/sina/options/tquote` | 已接入 | T 型报价 |
| `sina_option_greeks` | `/internal/a-stock/sina/options/greeks` | 已接入 | Greeks/IV |

### Baidu, iwencai, mootdx

| Dataset | Internal route | 今日状态 | 备注 |
| --- | --- | --- | --- |
| `baidu_kline_ma` | `/internal/a-stock/baidu/kline-ma` | 已接入 | K 线带 MA5/10/20 |
| `iwencai_search` | `/internal/a-stock/iwencai/search` | 已接入，live skipped | 需要 `IWENCAI_API_KEY` |
| `iwencai_query` | `/internal/a-stock/iwencai/query` | 已接入，live skipped | 语义查询 |
| `tdx_quotes` | `/internal/a-stock/tdx/quotes` | 已接入查询 | mootdx quote |
| `tdx_order_book` | `/internal/a-stock/tdx/order-book` | 已接入 | 五档盘口 |
| `tdx_transactions` | `/internal/a-stock/tdx/transactions` | 已接入 | 逐笔成交 |

## 2026-07-07 新增持久化能力

新增 job：`sync_a_stock_dataset`。

命令示例：

```powershell
cd E:\project-repo\cynexus\cynexus-data
$env:PYTHONPATH="src"
python -m cynexus_data.jobs.sync_a_stock_dataset --dry-run --dataset eastmoney_fund_flow_120d --code 600519 --payload '[{"date":"2026-07-07","main_net":"1.25"}]'
```

通过 Core scheduler 或 trigger API 调用时，核心参数如下：

| 参数 | 说明 |
| --- | --- |
| `dataset` | 单个 dataset key |
| `datasets` | 逗号分隔 dataset key |
| `codes` / `code` | 股票池 |
| `trade_date` / `snapshot_date` | 交易日或快照日 |
| `create_table` | 是否先执行 DDL |
| `fetch_live` | dry-run 默认不抓外网；传 true 可做连通性验证 |
| `request_sleep_seconds` | 覆盖默认反爬间隔 |
| `payload` / `payloads` | 测试或离线写入 payload |

## Dataset 到存储映射

| Dataset | 存储目标 |
| --- | --- |
| `eastmoney_stock_info` | PG `sec_stock_basic` 扩展字段 |
| `eastmoney_reports`, `eastmoney_industry_reports`, `iwencai_search`, `iwencai_query`, `ths_eps_forecast` | PG `a_stock_research_report` |
| `sina_financial_report` | PG `a_stock_financial_statement` |
| `cninfo_announcements` | PG `a_stock_announcement` |
| `eastmoney_stock_news`, `eastmoney_global_news` | PG `a_stock_news` |
| `cninfo_irm` | PG `a_stock_investor_interaction` |
| `eastmoney_fund_flow_120d` | PG `a_stock_fund_flow_daily` |
| `market_fund_flow_daily` | PG `market_overview_snapshots` |
| `eastmoney_fund_flow_minute` | CH `ch_ods_stock_fund_flow_minute` |
| `eastmoney_industry_rank` | PG `a_stock_industry_rank_snapshot` |
| `eastmoney_daily_dragon_tiger`, `eastmoney_dragon_tiger` | PG `a_stock_dragon_tiger_daily`, `a_stock_dragon_tiger_seat` |
| `eastmoney_lockup` | PG `a_stock_lockup_expiry` |
| `eastmoney_block_trade` | PG `a_stock_block_trade` |
| `eastmoney_holders` | PG `a_stock_holder_num` |
| `eastmoney_dividends` | PG `a_stock_dividend` |
| `eastmoney_hot_rank`, `ths_hot_list` | PG `a_stock_hot_rank_snapshot` |
| `eastmoney_hot_concept` | PG `a_stock_hot_concept_snapshot` |
| `eastmoney_concept_blocks` | PG `a_stock_concept_membership` |
| `ths_hot_reason` | PG `a_stock_hot_reason_snapshot` |
| `eastmoney_limit_pool`, `ths_limit_up_pool` | PG `a_stock_limit_pool_snapshot` |
| `sina_option_codes` | PG `a_stock_option_contract_snapshot` |
| `sina_option_tquote`, `sina_option_greeks` | CH `ch_ods_option_quote_snapshot` |
| `tdx_transactions` | CH `ch_ods_stock_transaction` |
| `tdx_order_book` | CH `ch_ods_stock_order_book_snapshot` |
| `baidu_kline_ma` | CH `ch_ods_baidu_kline_ma` |
| `ths_northbound` | CH `ch_ods_northbound_minute` |

## Live smoke 结果记录

最近一次 smoke：`cynexus-data/var/reports/a-stock-data-smoke/20260706-233539`。

结果：25 ok / 5 failed / 1 skipped。

失败：

- EastMoney `stock_info`
- EastMoney `fund_flow_minute`
- EastMoney `fund_flow_120d`
- EastMoney `industry_rank`
- EastMoney `hot_rank`

原因：当前网络/proxy 下 selected EastMoney `push2`/`push2his`/`emappdata` connection reset。它们属于上游连通性问题，不是 parser 或路由注册失败。

跳过：

- `iwencai_search`：未配置 `IWENCAI_API_KEY`。

## 后续功能可直接使用的能力

- 股票详情页可读取 PG 研报、公告、新闻、问答、股东户数、分红、解禁、大宗交易。
- 策略或盘中页面可读取 CH 分钟资金流、北向分钟流、盘口快照、逐笔成交、期权 Greeks。
- 概念/题材页面可读取 `a_stock_concept_membership`, `a_stock_hot_concept_snapshot`, `a_stock_hot_reason_snapshot`。
- 行业页面可同时比较 EastMoney 行业排名、THS/TDX 行业行情与资金流，但必须按 `source` 显示口径。
- AI 分析可把 `a_stock_news`, `a_stock_announcement`, `a_stock_research_report`, `a_stock_investor_interaction` 作为事件和文本素材。

## 不应混淆的口径

- EastMoney 行业排名不是 THS/TDX 行业资金流。
- EastMoney 120 日资金流不是已有 Tencent `stock_fund_flow_snapshot`。
- Baidu MA K 线是外部备份口径，不替代 ClickHouse 主 K 线。
- mootdx 逐笔成交不是交易所逐笔全量历史，只是行情服务器能返回的成交明细。
- eltdx F10 新闻/公告是摘要；CNInfo 公告是公告检索，后续可扩展全文下载。

## 待补前提

- 配置 `IWENCAI_API_KEY` 后才能验证 iwencai 语义研报 live。
- 如果要全市场高频资金流，需要先定义股票池、限速、失败重试和磁盘容量预算。
- 如果 UI 需要公告 PDF 全文，需要新增下载器、文件存储路径、全文抽取和索引任务。
