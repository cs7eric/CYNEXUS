# A Stock 数据落库表设计

本文档记录 `cynexus-data` 中 A 股扩展数据的落库表、来源、刷新策略和失败风险。表结构落在 PostgreSQL `cynexus_appl_market` 与 ClickHouse `cynexus`，DDL 位于：

- `cynexus-data/sql/postgres/20260707_create_a_stock_data_tables.sql`
- `cynexus-data/sql/clickhouse/20260707_create_a_stock_data_tables.sql`

## 分层约束

- `sources/*` 只负责 provider HTTP/TCP 调用和字段解析。
- `domain/stock/a_stock_persistence.py` 负责纯数据归一化，不访问数据库、网络、环境变量。
- `pipelines/a_stock/sync_a_stock_dataset.py` 负责编排抓取、限速、归一化、写库。
- `storage/postgres/a_stock.py` 和 `storage/clickhouse/a_stock.py` 负责数据库写入。
- `jobs/sync_a_stock_dataset.py` 是 executor 入口，已注册到 `jobs/registry.py` 和 `config/jobs.yaml`。

## PostgreSQL 表

### `sec_stock_basic` 扩展字段

用途：承接东财个股基本面补充字段，同时复用项目已有证券基础信息主表。

新增字段：

| 字段 | 含义 | 来源 |
| --- | --- | --- |
| `total_shares` | 总股本 | EastMoney `push2 stock/get` |
| `float_shares` | 流通股本 | EastMoney `push2 stock/get` |
| `list_date` | 上市日期 | EastMoney `push2 stock/get` |
| `eastmoney_price` | 东财快照价 | EastMoney |
| `eastmoney_float_market_cap` | 东财流通市值 | EastMoney |
| `eastmoney_updated_at` | 东财字段更新时间 | pipeline 写入时间 |

自然键：`code`。

### `a_stock_research_report`

用途：个股研报、行业研报、iwencai 语义研报、同花顺 EPS 一致预期原始记录。

来源：

- EastMoney `reportapi` 个股研报：`eastmoney_reports`
- EastMoney `reportapi` 行业研报：`eastmoney_industry_reports`
- iwencai 搜索：`iwencai_search` / `iwencai_query`
- THS EPS 一致预期：`ths_eps_forecast`

关键字段：`source`, `scope`, `code`, `industry_code`, `report_id`, `title`, `org_name`, `author`, `publish_date`, `rating`, `pdf_url`, `metrics`, `raw_payload`。

自然键：`source + scope + code + industry_code + report_id`。

调度建议：

- EastMoney reportapi：交易日盘后或每天 1-2 次，单请求间隔 2s。
- iwencai：需要 `IWENCAI_API_KEY`，建议按功能触发或每天小批量，间隔 3s 以上。
- THS EPS：每天 1 次即可，间隔 1s。

### `a_stock_financial_statement`

用途：新浪财务报表结构化快照。

来源：`sina_financial_report`。

关键字段：`source`, `code`, `report_type`, `report_date`, `fields`, `raw_payload`。

自然键：`source + code + report_type + report_date`。

调度建议：每天 1 次，或财报季加密到每 6 小时。

### `a_stock_announcement`

用途：巨潮公告检索结果，后续可扩展全文下载路径。

来源：`cninfo_announcements`。

关键字段：`source`, `code`, `announcement_id`, `title`, `category`, `publish_date`, `url`, `raw_payload`。

自然键：`source + code + announcement_id`。

调度建议：每天盘后 1 次；公告季可每 2-4 小时小批量，单请求间隔 1.5s。

### `a_stock_news`

用途：东财个股新闻、东财全球 7x24 资讯。

来源：

- `eastmoney_stock_news`
- `eastmoney_global_news`

关键字段：`source`, `code`, `news_id`, `title`, `summary`, `media_name`, `published_at`, `url`, `raw_payload`。

自然键：`source + code + news_id`。

调度建议：

- 个股新闻：按自选池或关注池每 30-60 分钟。
- 全球 7x24：若 UI 需要滚动资讯，每 5-15 分钟；否则每小时。
- EastMoney 请求间隔 2s，避免并发。

### `a_stock_investor_interaction`

用途：互动易/巨潮 IRM 问答。

来源：`cninfo_irm`。

关键字段：`source`, `code`, `interaction_id`, `company_name`, `question`, `answer`, `answerer`, `asked_at`, `raw_payload`。

自然键：`source + code + interaction_id`。

调度建议：每天 1 次，重点股票可每 6 小时，单请求间隔 1.5s。

### `a_stock_fund_flow_daily`

用途：东财 120 日个股资金流日级数据。项目已有腾讯 `stock_fund_flow_snapshot`，此表保留 EastMoney 口径，避免混淆。

来源：`eastmoney_fund_flow_120d`。

关键字段：`source`, `code`, `trade_date`, `main_net`, `small_net`, `mid_net`, `large_net`, `super_net`, `raw_payload`。

自然键：`source + code + trade_date`。

调度建议：主要服务 UI 时每天盘后 1 次；若盘中展示日内累计变化，可每 30-60 分钟小池同步。

### `a_stock_industry_rank_snapshot`

用途：东财行业板块排名快照，与已有 THS/TDX 行业行情和资金流区分 `source`。

来源：`eastmoney_industry_rank`。

关键字段：`source`, `snapshot_date`, `rank_direction`, `rank_no`, `industry_code`, `industry_name`, `change_pct`, `up_count`, `down_count`, `leader_name`。

自然键：`source + snapshot_date + rank_direction + rank_no + industry_code`。

调度建议：盘中每 15-30 分钟；盘后保留收盘快照。EastMoney push2 端点容易被重置，必须串行，间隔 2s 以上。

### `a_stock_dragon_tiger_daily`

用途：龙虎榜全市场每日股票记录和单股历史记录。

来源：

- `eastmoney_daily_dragon_tiger`
- `eastmoney_dragon_tiger`

关键字段：`source`, `trade_date`, `code`, `name`, `reason`, `close_price`, `change_pct`, `net_buy_wan`, `buy_wan`, `sell_wan`, `turnover_pct`。

自然键：`source + trade_date + code + reason`。

调度建议：交易日 18:00 后 1 次。

### `a_stock_dragon_tiger_seat`

用途：龙虎榜买卖席位明细。

来源：`eastmoney_dragon_tiger`。

关键字段：`source`, `trade_date`, `code`, `side`, `seat_rank`, `seat_name`, `buy_wan`, `sell_wan`, `net_wan`。

自然键：`source + trade_date + code + side + seat_rank + seat_name`。

调度建议：按需要对上榜股票拉取，交易日 18:00 后，单请求间隔 2s。

### `a_stock_lockup_expiry`

用途：限售解禁历史与未来日历。

来源：`eastmoney_lockup`。

关键字段：`source`, `code`, `free_date`, `lockup_type`, `shares`, `ratio`。

自然键：`source + code + free_date + lockup_type`。

调度建议：每周 1 次全量关注池，或个股详情页按需刷新。

### `a_stock_block_trade`

用途：大宗交易。

来源：`eastmoney_block_trade`。

关键字段：`source`, `code`, `trade_date`, `price`, `close_price`, `premium_pct`, `volume`, `amount`, `buyer`, `seller`。

自然键：`source + code + trade_date + price + buyer + seller`。

调度建议：交易日盘后 1 次。

### `a_stock_holder_num`

用途：股东户数变化。

来源：`eastmoney_holders`。

关键字段：`source`, `code`, `end_date`, `holder_num`, `change_num`, `change_ratio`, `avg_shares`。

自然键：`source + code + end_date`。

调度建议：每周 1-2 次。

### `a_stock_dividend`

用途：分红送转历史。

来源：`eastmoney_dividends`。

关键字段：`source`, `code`, `ex_dividend_date`, `bonus_rmb`, `transfer_ratio`, `bonus_ratio`, `plan`。

自然键：`source + code + ex_dividend_date + plan`。

调度建议：每周 1 次，财报季每天 1 次。

### `a_stock_hot_rank_snapshot`

用途：东财人气榜、同花顺热榜。

来源：

- `eastmoney_hot_rank`
- `ths_hot_list`

关键字段：`source`, `snapshot_date`, `list_type`, `rank_no`, `code`, `name`, `heat`, `price`, `pct`, `rank_change`, `concepts`。

自然键：`source + snapshot_date + list_type + rank_no + code`。

调度建议：盘中每 15-30 分钟；EastMoney hot rank 使用 `emappdata`，失败率较高，间隔建议 5s 且不并发。

### `a_stock_hot_concept_snapshot`

用途：东财热门概念命中。

来源：`eastmoney_hot_concept`。

关键字段：`source`, `snapshot_date`, `code`, `concept_code`, `concept_name`, `hit_count`。

自然键：`source + snapshot_date + code + concept_code + concept_name`。

调度建议：仅对关注池/榜单股票按需刷新，间隔 5s。

### `a_stock_hot_reason_snapshot`

用途：同花顺热点强势股/题材归因。

来源：`ths_hot_reason`。

关键字段：`source`, `snapshot_date`, `code`, `name`, `reason_key`, `reason`, `subject`。

自然键：`source + snapshot_date + code + reason_key`。

调度建议：盘中每 15-30 分钟；盘后保留收盘快照。

### `a_stock_limit_pool_snapshot`

用途：东财涨停池/炸板池/跌停池/昨日涨停池、同花顺涨停池。

来源：

- `eastmoney_limit_pool`
- `ths_limit_up_pool`

关键字段：`source`, `snapshot_date`, `pool`, `code`, `name`, `price`, `pct`, `reason`, `board_type`, `limit_days`, `first_time`, `last_time`, `break_times`, `seal_amount`。

自然键：`source + snapshot_date + pool + code`。

调度建议：盘中每 5-15 分钟，收盘后补一次。东财和 THS 需要分 source 保留口径。

### `a_stock_option_contract_snapshot`

用途：新浪 ETF 期权合约清单。

来源：`sina_option_codes`。

关键字段：`source`, `snapshot_date`, `underlying`, `option_month`, `option_type`, `option_code`。

自然键：`source + snapshot_date + underlying + option_month + option_type + option_code`。

调度建议：每天开盘前 1 次；合约月切换周可每天 2 次。

### `a_stock_concept_membership`

用途：东财 slist 个股概念归属。

来源：`eastmoney_concept_blocks`。

关键字段：`source`, `snapshot_date`, `code`, `concept_code`, `concept_name`, `change_pct`, `lead_stock`。

自然键：`source + snapshot_date + code + concept_code + concept_name`。

调度建议：每天 1 次；概念页面按需刷新。

## ClickHouse 表

### `ch_ods_stock_fund_flow_minute`

用途：东财个股分钟级资金流。

来源：`eastmoney_fund_flow_minute`。

分区：`toYYYYMM(trade_date)`。

排序键：`source, code, trade_time`。

调度建议：盘中每 5-15 分钟，但只跑关注池/榜单池；全市场分钟级不建议直接公开接口扫。

### `ch_ods_stock_transaction`

用途：mootdx 逐笔成交。

来源：`tdx_transactions`。

典型数据：成交时间、成交价、成交量、成交额、买卖方向或中性标记，字段因服务器返回而异，完整原始字段保存在 `raw_payload`。

分区：`toYYYYMM(trade_date)`。

调度建议：按需或关注池盘中采样；mootdx 是 TCP 行情源，IP 封禁风险低于 Web 反爬，但服务器资源有限，建议串行或小并发。

### `ch_ods_stock_order_book_snapshot`

用途：mootdx 五档盘口快照。

来源：`tdx_order_book`。

字段：价格、开高低昨收、成交量额、买卖五档价量 JSON。

调度建议：策略需要时 5-30 秒采样；普通 UI 使用实时查询即可，不必高频落库。

### `ch_ods_baidu_kline_ma`

用途：百度股市通 K 线及 MA5/10/20。

来源：`baidu_kline_ma`。

分区：`toYear(trade_date)`。

调度建议：每天盘后 1 次，作为带均线字段的外部口径备份，不替代项目主 K 线。

### `ch_ods_northbound_minute`

用途：同花顺北向资金分钟流向。

来源：`ths_northbound`。

调度建议：盘中每 1-5 分钟；可配本地缓存，避免页面重复打源站。

### `ch_ods_option_quote_snapshot`

用途：新浪 ETF 期权 T 型报价、希腊字母、IV 快照。

来源：

- `sina_option_tquote`
- `sina_option_greeks`

调度建议：策略或观察页需要时 30-60 秒；否则按需查询。

## 复用既有表

以下能力已有明确项目表或 pipeline，不新增平行表：

| 能力 | 既有位置 | 说明 |
| --- | --- | --- |
| 主 K 线和复权 K 线 | ClickHouse `daily_raw`, `daily_qfq`, `daily_hfq`, `ch_dwd_kline_1m` | 百度 MA 表只作为外部口径补充 |
| 融资融券 | 既有 margin pipeline/table | EastMoney margin 不另建主表，避免与 exchange/akshare 口径混写 |
| THS/TDX 行业资金流 | `sector_fund_flow_daily_snapshots`, `mkt_ths_industry_fund_flow_daily`, TDX industry flow tables | 东财行业排名单独建快照表 |
| 既有市场涨停情绪 | `market_limit_daily_snapshots`, `market_limit_daily_stocks`, MSI limit emotion tables | 东财/THS 原始池子落 `a_stock_limit_pool_snapshot` 作为 source 口径 |
| F10 新闻/公告摘要 | `cynexus-data` F10 query routes | 东财新闻、巨潮公告另落上表 |

## 反爬与调度间隔

| 来源 | 默认单请求间隔 | 并发 | 建议 |
| --- | ---: | --- | --- |
| EastMoney datacenter/reportapi | 2s | 不并发 | 日级/盘后优先；失败要记录 endpoint 和异常 |
| EastMoney push2/push2his/emappdata | 2-5s | 不并发 | 易 connection reset；分钟资金流和热榜只跑小池 |
| THS/hexin | 1s | 小并发或串行 | 热点和涨停池可 15-30 分钟；北向可 1-5 分钟 |
| CNInfo | 1.5s | 串行 | 公告/IRM 日级或低频增量 |
| Sina | 0.5s | 小并发 | 财务日级；期权报价 30-60 秒 |
| Baidu | 1s | 串行 | 日级即可 |
| iwencai | 3s+ | 串行 | API key/配额敏感，建议按需或小批量 |
| mootdx | 0.2s | 小并发 | TCP 行情源，控制关注池规模 |

## 当前接入状态

- 表 DDL：已落到 `cynexus-data/sql/postgres` 与 `cynexus-data/sql/clickhouse`。
- 仓储：已新增 `AStockPostgresRepository` 与 `AStockClickHouseRepository`。
- Pipeline：已新增 `sync_a_stock_dataset`，支持 `dataset`/`datasets`、`codes`、`create_table`、`fetch_live`、`request_sleep_seconds`。
- Job：已注册 `sync_a_stock_dataset`，并加入 `config/jobs.yaml`。
- 单元测试：覆盖归一化与 dry-run pipeline。

## 已知风险

- EastMoney `push2`, `push2his`, `emappdata` 在 2026-07-06 smoke 中出现 connection reset，需要在调度侧保留失败记录和重试退避。
- iwencai 需要 `IWENCAI_API_KEY`，当前无 key 时不能做真实连通性验证。
- mootdx 逐笔字段会随服务器实现变化，`raw_payload` 是事实保真字段，结构化列只取通用字段。
