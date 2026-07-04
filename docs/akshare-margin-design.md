# AkShare Margin Data Design

## 背景

`cynexus-data` 目前已经注册了 `sync_margin` job，但 `pipelines/margin/sync_margin.py` 仍是占位实现，没有真正接入两融数据。

当前 AkShare 数据源已在 `sources/akshare` 下接入部分行情与资金流接口，两融相关能力应继续遵循这一分层：

- `sources/akshare`: 只处理 AkShare 调用与字段归一化
- `pipelines/margin`: 处理业务编排、参数、dry-run、质量统计
- `storage/postgres`: 负责落库、幂等、软删除、查询
- `jobs`: 只作为 job runner 入口，不放业务逻辑

## 目标

实现一个可定时、可手动、可重复执行的 `sync_margin` pipeline，用于同步 A 股两融数据。

首期覆盖：

- 交易所两融汇总数据
- 个股两融明细数据
- 两融账户统计数据

首期不覆盖：

- 保证金比例历史建模
- 北交所两融明细
- Java API / 前端展示

## AkShare 接口

当前本地 AkShare 版本 `1.18.46` 可用接口如下。

### 交易所汇总

`stock_margin_sse(start_date, end_date)`

- 来源：上交所
- 粒度：交易日 + 交易所
- 用途：沪市两融汇总
- 主要字段：
  - 信用交易日期
  - 融资余额
  - 融资买入额
  - 融券余量
  - 融券余量金额
  - 融券卖出量
  - 融资融券余额

`stock_margin_szse(date)`

- 来源：深交所
- 粒度：交易日 + 交易所
- 用途：深市两融汇总
- 主要字段：
  - 融资买入额
  - 融资余额
  - 融券卖出量
  - 融券余量
  - 融券余额
  - 融资融券余额

### 个股明细

`stock_margin_detail_sse(date)`

- 来源：上交所
- 粒度：交易日 + 交易所 + 股票代码
- 用途：沪市个股两融明细
- 主要字段：
  - 信用交易日期
  - 标的证券代码
  - 标的证券简称
  - 融资余额
  - 融资买入额
  - 融资偿还额
  - 融券余量
  - 融券卖出量
  - 融券偿还量

`stock_margin_detail_szse(date)`

- 来源：深交所
- 粒度：交易日 + 交易所 + 股票代码
- 用途：深市个股两融明细
- 主要字段：
  - 证券代码
  - 证券简称
  - 融资买入额
  - 融资余额
  - 融券卖出量
  - 融券余量
  - 融券余额
  - 融资融券余额

### 账户统计

`stock_margin_account_info()`

- 来源：东方财富
- 粒度：交易日
- 用途：市场级两融账户统计
- 主要字段：
  - 日期
  - 融资余额
  - 融券余额
  - 融资买入额
  - 融券卖出额
  - 证券公司数量
  - 营业部数量
  - 个人投资者数量
  - 机构投资者数量
  - 参与交易的投资者数量
  - 有融资融券负债的投资者数量
  - 担保物总价值
  - 平均维持担保比例

## 数据模型

### `margin_market_summary`

市场/交易所级汇总表。

唯一键：

```text
trade_date + exchange + source
```

字段：

```text
id bigint primary key
trade_date date not null
exchange text not null              -- SSE / SZSE
source text not null                -- akshare.stock_margin_sse / akshare.stock_margin_szse
financing_balance numeric(20, 4)
financing_buy_amount numeric(20, 4)
financing_repay_amount numeric(20, 4)
securities_lending_balance numeric(20, 4)
securities_lending_balance_amount numeric(20, 4)
securities_lending_sell_volume numeric(20, 4)
securities_lending_repay_volume numeric(20, 4)
margin_balance numeric(20, 4)
extra jsonb not null default '{}'::jsonb
created_at timestamptz not null default now()
updated_at timestamptz not null default now()
deleted_at timestamptz
```

### `margin_stock_detail`

个股两融明细表。

唯一键：

```text
trade_date + exchange + stock_code + source
```

字段：

```text
id bigint primary key
trade_date date not null
exchange text not null              -- SSE / SZSE
stock_code text not null
stock_name text
source text not null
financing_balance numeric(20, 4)
financing_buy_amount numeric(20, 4)
financing_repay_amount numeric(20, 4)
securities_lending_balance numeric(20, 4)
securities_lending_balance_amount numeric(20, 4)
securities_lending_sell_volume numeric(20, 4)
securities_lending_repay_volume numeric(20, 4)
margin_balance numeric(20, 4)
extra jsonb not null default '{}'::jsonb
created_at timestamptz not null default now()
updated_at timestamptz not null default now()
deleted_at timestamptz
```

### `margin_account_stats`

两融账户统计表。

唯一键：

```text
trade_date + source
```

字段：

```text
id bigint primary key
trade_date date not null
source text not null                -- akshare.stock_margin_account_info
financing_balance numeric(20, 4)
securities_lending_balance numeric(20, 4)
financing_buy_amount numeric(20, 4)
securities_lending_sell_amount numeric(20, 4)
broker_count integer
business_department_count integer
personal_investor_count integer
institution_investor_count integer
active_investor_count integer
liability_investor_count integer
total_guarantee numeric(20, 4)
average_guarantee_ratio numeric(20, 6)
extra jsonb not null default '{}'::jsonb
created_at timestamptz not null default now()
updated_at timestamptz not null default now()
deleted_at timestamptz
```

## 源数据封装

新增文件：

```text
cynexus-data/src/cynexus_data/sources/akshare/margin.py
```

职责：

- 调用 AkShare 两融接口
- 将中文字段转换为项目内部字段
- 统一日期格式为 `date`
- 统一交易所枚举为 `SSE` / `SZSE`
- 对 numeric 字段做 `None` 安全转换
- 输出 list[dict]，不落库

建议函数：

```python
fetch_margin_market_summary(ak, trade_date: date) -> list[dict]
fetch_margin_stock_detail(ak, trade_date: date) -> list[dict]
fetch_margin_account_stats(ak, trade_date: date | None) -> list[dict]
```

扩展：

```text
cynexus-data/src/cynexus_data/sources/akshare/client.py
```

新增方法：

```python
fetch_margin_market_summary(trade_date: date) -> list[dict]
fetch_margin_stock_detail(trade_date: date) -> list[dict]
fetch_margin_account_stats(trade_date: date | None = None) -> list[dict]
```

## Pipeline 设计

文件：

```text
cynexus-data/src/cynexus_data/pipelines/margin/sync_margin.py
```

参数：

```text
trade_date: optional
providers: optional, default ["akshare"]
datasets: optional, default ["summary", "detail", "account"]
create_table: optional
```

执行逻辑：

1. 解析 `trade_date`，默认取今天。
2. 解析 `datasets`：
   - `summary`: 同步交易所汇总
   - `detail`: 同步个股明细
   - `account`: 同步账户统计
3. dry-run 只返回计划，不访问 AkShare / Postgres。
4. 非 dry-run：
   - 通过 `context.hooks.akshare.get_client()` 获取 client
   - 通过 `context.hooks.postgres.get_client()` 获取 Postgres client
   - 按 dataset 拉取数据
   - 调用 repository upsert
5. 返回统一结果：

```json
{
  "pipeline": "margin.sync_margin",
  "dry_run": false,
  "trade_date": "2026-07-04",
  "providers": ["akshare"],
  "datasets": ["summary", "detail", "account"],
  "rows_read": 0,
  "rows_written": 0,
  "summary_rows": 0,
  "detail_rows": 0,
  "account_rows": 0
}
```

## Storage 设计

新增文件：

```text
cynexus-data/src/cynexus_data/storage/postgres/margin.py
```

职责：

- `create_table()`
- `replace_market_summary(trade_date, rows)`
- `replace_stock_detail(trade_date, rows)`
- `replace_account_stats(rows)`

落库策略：

- 使用软删除 + insert 或 `ON CONFLICT DO UPDATE`。
- 这里建议用 `ON CONFLICT DO UPDATE`，因为两融数据按自然唯一键稳定，重复执行应覆盖最新值。
- `id` 仍使用 `next_bigint_id()`。
- 如果主键 id 冲突，使用已有的 `_execute_generated_id_insert` 思路重试，避免 snowflake 极端冲突。

DDL 文件：

```text
cynexus-data/sql/postgres/20260704_create_margin_tables.sql
```

## Job 配置

已有：

```text
cynexus-data/src/cynexus_data/jobs/sync_margin.py
cynexus-data/src/cynexus_data/jobs/registry.py
cynexus-data/src/cynexus_data/config/jobs.yaml
```

需要补充 `jobs.yaml` 参数说明：

```yaml
sync_margin:
  description: Sync margin trading data from AkShare.
  params:
    trade_date: optional
    providers: optional
    datasets: optional
    create_table: optional
```

调度建议：

- 执行时间：交易日 17:30 以后
- 原因：交易所两融数据通常在盘后更新，过早执行可能拿到空数据或上一交易日数据。

## 幂等与质量

幂等要求：

- 同一个 `trade_date` 重跑不会重复有效数据。
- 同一个 `trade_date + exchange + stock_code + source` 只保留一条有效明细。
- 数据为空时不应覆盖已有有效数据，除非显式传入 `allow_empty_replace=true`。

质量检查：

- summary 至少包含 `SSE` / `SZSE` 中一个市场。
- detail 行数为 0 时标记 warning。
- account 数据如果 AkShare 返回全量历史，只筛选 `trade_date` 当日；如果没有当日数据，不写入。
- numeric 字段全部允许 null，但不允许字符串原样入库。

## 测试计划

新增单测：

```text
tests/unit/test_sources/test_akshare_margin.py
tests/unit/test_pipelines/test_margin_pipeline.py
tests/unit/test_storage/test_postgres_margin.py
```

覆盖：

- SSE/SZSE summary 字段归一化
- SSE/SZSE detail 字段归一化
- account stats 字段归一化
- dry-run 不访问 hooks
- pipeline 按 datasets 分流
- storage upsert SQL 包含自然唯一键
- 空数据不覆盖旧数据

## 后续扩展

第二阶段可以接：

- `stock_margin_underlying_info_szse(date)`: 深市两融标的证券信息
- `stock_margin_ratio_pa(symbol, date)`: 标的保证金比例
- Java API：暴露最新两融汇总、个股排行、账户统计趋势
- UI：在市场概览或 Data Source 页面展示两融数据 freshness
