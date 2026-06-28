可以，把 **Connections / Hooks** 融进去之后，结构建议调整成下面这样。

核心变化是：

```text
Connections = 外部系统连接配置 / 凭证 / endpoint / database / token / timeout
Hooks       = 根据 Connection 创建可用客户端，比如 ClickHouse client、Postgres engine、EastMoney session、TDX reader
Sources     = 只负责业务数据源适配，不直接读 env、不直接 new 数据库连接
Storage     = 只负责读写表，但连接来自 Hook
Pipelines   = 编排 source → transform → storage
```

---

```text
cynexus-data/
  pyproject.toml
  README.md
  .env.example

  src/
    cynexus_data/
      __init__.py

      config/
        settings.py                 # 全局配置入口，读取 env / yaml
        connections.yaml            # 外部系统连接声明，非敏感配置
        sources.yaml                # 数据源开关、频率、限流、fallback 规则
        database.yaml               # 表名、schema、库名、写入策略

      core/
        calendar.py                 # 交易日历工具
        types.py                    # 通用类型定义
        exceptions.py               # 项目异常
        idempotency.py              # 幂等 key、去重逻辑
        retry.py                    # 重试策略
        logging.py                  # 日志封装
        time.py                     # 时间、时区、交易时间窗口
        constants.py                # 常量

      connections/
        __init__.py
        models.py                   # Connection 对象定义
        registry.py                 # ConnectionRegistry，按 conn_id 获取连接配置
        resolver.py                 # 从 yaml/env/Nacos 等解析连接
        secrets.py                  # 密码、token、secret 的读取抽象
        factory.py                  # 根据 connection type 创建连接对象
        validators.py               # 校验连接配置是否合法

      hooks/
        __init__.py
        base.py                     # BaseHook
        http.py                     # BaseHttpHook，封装 requests/httpx session
        database.py                 # BaseDatabaseHook

        sources/
          tdx.py                    # TdxHook
          eastmoney.py              # EastMoneyHook
          akshare.py                # AkshareHook
          sina.py                   # SinaHook
          tencent.py                # TencentHook

        storage/
          clickhouse.py             # ClickHouseHook
          postgres.py               # PostgresHook
          duckdb.py                 # DuckDBHook
          redis.py                  # RedisHook，可选，后面做锁/缓存用

      sources/
        tdx/
          client.py                 # TDX 原始 client，依赖 TdxHook 提供的连接能力
          parser.py                 # 解析 TDX 原始数据
          schema.py                 # TDX 原始字段定义
          adapter.py                # 转成项目统一 raw record

        eastmoney/
          client.py
          parser.py
          schema.py
          adapter.py

        akshare/
          client.py
          adapter.py

        sina/
          client.py
          parser.py
          adapter.py

        tencent/
          client.py
          parser.py
          adapter.py

      storage/
        clickhouse/
          client.py                 # ClickHouse 轻封装 client
          writers.py                # 写入 ClickHouse
          readers.py                # 从 ClickHouse 查询
          ddl.py                    # ClickHouse 建表语句
          mutations.py              # 修复、删除、替换、重算相关操作

        postgres/
          client.py                 # Postgres 轻封装 client
          repositories.py           # Repository 层
          ddl.py                    # Postgres 建表语句
          upserts.py                # upsert 逻辑

        duckdb/
          client.py
          writers.py
          readers.py

      datasets/
        raw/
          kline_raw.py              # raw kline 数据定义
          fund_flow_raw.py
          margin_raw.py
          stock_basic_raw.py
          trade_calendar_raw.py

        staging/
          kline_staging.py          # 标准化后的 kline
          fund_flow_staging.py
          margin_staging.py
          stock_basic_staging.py
          trade_calendar_staging.py

        marts/
          kline_mart.py             # 面向消费的 kline 结果
          market_sentiment_mart.py
          sector_mart.py
          stock_snapshot_mart.py
          fund_flow_rank_mart.py

      domain/
        stock/
          adjust_price.py           # 前复权 / 后复权 / 不复权
          aggregate_kline.py        # 1m -> 5m/15m/30m/60m
          trading_calendar.py
          stock_universe.py

        market/
          msi.py                    # 市场情绪指数
          limit_emotion.py
          sector_breadth.py
          fund_flow.py
          margin.py

        indicators/
          ma.py
          volatility.py
          turnover.py
          amplitude.py
          momentum.py

      pipelines/
        kline/
          sync_daily.py             # 同步日线
          sync_minute.py            # 同步分钟线
          aggregate_periods.py      # 聚合 5m/15m/30m/60m
          backfill.py               # 历史补全
          repair.py                 # 修复缺失数据

        fund_flow/
          sync_main_force.py
          sync_sector_flow.py
          sync_stock_flow.py

        margin/
          sync_margin.py

        stock_basic/
          sync_stock_basic.py
          sync_trade_calendar.py

        msi/
          build_msi.py
          rebuild_msi.py

      jobs/
        daily_after_close.py        # 收盘后任务
        minute_realtime.py          # 盘中分钟任务
        backfill_history.py         # 历史补数据
        repair_missing_data.py      # 修复任务
        sync_metadata.py            # 股票基础信息、交易日历等
        health_check.py             # 数据源、数据库健康检查

      quality/
        contracts.py                # 数据契约
        checks.py                   # 完整性检查
        reconciliation.py           # 多数据源对账
        freshness.py                # 数据新鲜度检查
        anomaly.py                  # 异常值检查

      observability/
        job_log.py                  # 任务日志
        metrics.py                  # 指标采集
        alert.py                    # 告警
        lineage.py                  # 数据血缘，可选
        audit.py                    # 数据变更审计

      runtime/
        context.py                  # PipelineContext，统一持有 hooks / settings / logger
        locks.py                    # 分布式锁，本地锁，防止任务重复跑
        checkpoints.py              # 任务断点、游标、checkpoint
        scheduler.py                # 本地调度封装，可选

      cli.py

  tests/
    unit/
      test_connections/
      test_hooks/
      test_sources/
      test_domain/
      test_quality/

    integration/
      test_clickhouse/
      test_postgres/
      test_eastmoney/
      test_tdx/

  scripts/
    init_clickhouse.py
    init_postgres.py
    backfill_kline.py
    repair_trade_date.py
    check_connections.py
    check_data_quality.py

  docs/
    architecture/
      layering.md
      connections_hooks.md

    data_contracts/
      kline.md
      fund_flow.md
      margin.md
      msi.md

    table_design/
      clickhouse_tables.md
      postgres_tables.md

    pipeline_design/
      daily_after_close.md
      minute_realtime.md
      backfill_history.md

  deploy/
    local/
      docker-compose.yml

    airflow/
      dags/
        daily_after_close_dag.py
        minute_realtime_dag.py

    dagster/
      definitions.py
      assets/
      jobs/
```

---

## 重点新增的两层

### 1. `connections/`

这一层只管 **连接配置本身**。

比如：

```yaml
# config/connections.yaml

connections:
  clickhouse_default:
    type: clickhouse
    host: ${CLICKHOUSE_HOST}
    port: ${CLICKHOUSE_PORT}
    database: market
    username: ${CLICKHOUSE_USER}
    password: ${CLICKHOUSE_PASSWORD}

  postgres_default:
    type: postgres
    host: ${POSTGRES_HOST}
    port: ${POSTGRES_PORT}
    database: cynexus
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}

  eastmoney_default:
    type: http
    base_url: https://push2.eastmoney.com
    timeout: 10
    retry: 3

  tdx_local:
    type: filesystem
    root_path: ${TDX_DATA_PATH}
```

这里不要写业务逻辑，只描述：

```text
我有哪些外部系统
怎么连接它们
连接名是什么
凭证从哪里来
timeout / retry / pool_size 是多少
```

---

### 2. `hooks/`

这一层根据 `conn_id` 创建真正可用的客户端。

比如：

```python
# hooks/storage/clickhouse.py

class ClickHouseHook(BaseHook):
    conn_id = "clickhouse_default"

    def get_client(self):
        conn = self.get_connection(self.conn_id)
        return ClickHouseClient(
            host=conn.host,
            port=conn.port,
            username=conn.username,
            password=conn.password,
            database=conn.database,
        )
```

使用时：

```python
clickhouse = ClickHouseHook("clickhouse_default").get_client()
```

EastMoney 类似：

```python
# hooks/sources/eastmoney.py

class EastMoneyHook(BaseHttpHook):
    conn_id = "eastmoney_default"

    def get_client(self):
        session = self.get_session()
        return EastMoneyClient(session=session)
```

---

## 融入之后，调用链应该变成这样

不要这样：

```python
# 不推荐

def sync_minute():
    ch = clickhouse_connect.get_client(
        host=os.getenv("CLICKHOUSE_HOST"),
        password=os.getenv("CLICKHOUSE_PASSWORD"),
    )

    resp = requests.get("https://push2.eastmoney.com/xxx")
    ...
```

应该这样：

```python
# 推荐

def sync_minute(context: PipelineContext):
    eastmoney = context.hooks.eastmoney.get_client()
    clickhouse = context.hooks.clickhouse.get_client()

    raw_records = eastmoney.fetch_minute_kline(...)
    normalized = normalize_kline(raw_records)

    clickhouse.write_kline_1m(normalized)
```

也就是：

```text
pipeline 不直接知道密码
pipeline 不直接知道 host
pipeline 不直接 new requests session
pipeline 不直接 new clickhouse client

pipeline 只拿 hook 提供的可用 client
```

---

## 最终依赖关系

建议你记住这个方向：

```text
jobs
  ↓
pipelines
  ↓
sources / storage / domain / quality
  ↓
hooks
  ↓
connections
  ↓
config
```

更细一点：

```text
jobs
  ↓
PipelineContext
  ↓
Hooks
  ↓
Connections
  ↓
外部系统
```

而业务数据流是：

```text
source client
  ↓
parser / adapter
  ↓
raw dataset
  ↓
staging dataset
  ↓
domain calculation
  ↓
mart dataset
  ↓
storage writer
```

---

## 这样设计的好处

第一，后面你换连接方式很方便。

比如现在 ClickHouse 在本机：

```text
clickhouse_default -> localhost
```

以后换 Docker、云服务器、集群，只改：

```text
config/connections.yaml
.env
```

不用改 pipeline 代码。

第二，后面接 Nacos 也很自然。

现在：

```text
.env + yaml
```

以后可以变成：

```text
NacosConnectionResolver
```

但是上层代码不变。

第三，数据源可以做 fallback。

比如：

```text
1m K 线优先 TDX
TDX 缺失则 EastMoney
EastMoney 异常则 Tencent
```

这个可以放在：

```text
config/sources.yaml
pipelines/kline/sync_minute.py
hooks/sources/
```

第四，测试会简单很多。

你可以 mock hook：

```python
context.hooks.eastmoney = FakeEastMoneyHook()
context.hooks.clickhouse = FakeClickHouseHook()
```

然后单测 pipeline，不需要真的访问外网和数据库。

---
这个结构比较适合你现在的 cynexus-data。后面无论你接 Airflow、Dagster、Nacos、Redis、ClickHouse 集群，还是 Java 调 Python 任务，都不会推倒重来。
