cynexus-data 整体迁移 Plan
1. 新定位

cynexus-data 的定位不是：

旧 Flask backend
旧 scheduler 中心
旧 AI 服务
旧 UI API

而是：

Python Job Executor

它负责：

1. 数据源连接
2. 数据采集
3. 数据解析
4. 数据标准化
5. 数据计算
6. 数据写入 CH / DuckDB / PG
7. 数据质量检查
8. 数据修复 / 回填
9. job 执行入口
10. 提供给 Java 触发 job 的内部接口
11. 保留 MP4/音频转文字这个 job，但不展开 AI 后处理

Java 负责：

1. scheduler 配置
2. scheduler 状态
3. job 编排策略
4. AI provider
5. polish
6. summary
7. ask / Q&A
8. UI API
9. 业务权限
10. 前端 response 拼装

所以 Python 这边只需要回答一个问题：

Java 触发某个 job 后，Python 如何稳定完成数据/转写生产，并把结果写入数据库？
2. 迁移边界
   2.1 必须迁移

从老项目迁这些：

backend/adapters/market/
backend/db/
backend/repositories/market/
backend/repositories/stock/ 中和数据生产有关的部分
backend/repositories/scheduler/ 中 job_run / checkpoint 相关部分
backend/services/stock/
backend/services/scheduler/ 中 job 执行动作
backend/scripts/ 中数据初始化、修复、回填脚本
design/backend/ 中数据迁移、表设计、pipeline 说明

能力范围：

EastMoney
TDX / eltdx / mootdx
Sina
Tencent
THS
AKShare fallback
ClickHouse 读写
DuckDB 读写
Postgres 读写
日线 K
分钟 K
K线聚合
股票基础信息
交易日历
股票池
资金流
行业 / 概念
市场宽度
涨跌停情绪
market pulse
market overview
market sentiment
MSI 九因子
数据质量检查
数据修复
历史回填
job 执行
job trigger 接口
MP4/音频转文字 job
2.2 不迁移

不迁这些：

backend/api 里的 UI API
Flask app_factory / bootstrap 作为应用主体
AI provider 管理
AI adapter service
polish
summary
ask
prompt
chat
completion
前端 response 拼装
scheduler cron 配置
scheduler enable/disable 状态
旧 JSON runtime 持久化方案
uploads/outputs 作为旧运行机制

注意：
transcription_service.py 只允许迁 “媒体转文字” 这一步；后续 AI polish / summary / ask 不进 Python。

3. 新项目结构需要微调

你原来的结构大体没问题，但建议加三个小模块：

interfaces/
runtime/job_runner.py
jobs/registry.py

最终结构建议是：

src/
cynexus_data/
config/
settings.py
connections.yaml
sources.yaml
database.yaml
jobs.yaml

    core/
    connections/
    hooks/
    sources/
    storage/
    datasets/
    domain/
    pipelines/
    quality/
    observability/

    runtime/
      context.py
      locks.py
      checkpoints.py
      job_runner.py
      job_store.py
      idempotency.py

    jobs/
      contracts.py
      registry.py
      health_check.py
      sync_metadata.py
      sync_trade_calendar.py
      sync_stock_basic.py
      sync_kline_daily.py
      sync_kline_minute.py
      aggregate_kline_periods.py
      sync_fund_flow.py
      sync_margin.py
      build_msi.py
      rebuild_msi.py
      backfill_history.py
      repair_missing_data.py
      quality_check.py
      transcribe_media.py

    interfaces/
      trigger_api/
        app.py
        routes.py
        schemas.py
        auth.py
        errors.py

transcribe_media.py 只作为一个 job 存在，不要让它影响主架构。

4. 老项目到新项目映射
   4.1 adapters 迁移
   backend/adapters/market/eastmoney.py
   -> hooks/sources/eastmoney.py
   -> sources/eastmoney/client.py
   -> sources/eastmoney/parser.py
   -> sources/eastmoney/adapter.py

backend/adapters/market/tencent.py
-> hooks/sources/tencent.py
-> sources/tencent/client.py
-> sources/tencent/parser.py
-> sources/tencent/adapter.py

backend/adapters/market/sina.py
-> hooks/sources/sina.py
-> sources/sina/client.py
-> sources/sina/parser.py
-> sources/sina/adapter.py

backend/adapters/market/eltdx_adapter.py
backend/adapters/market/mootdx_adapter.py
-> hooks/sources/tdx.py
-> sources/tdx/client.py
-> sources/tdx/parser.py
-> sources/tdx/adapter.py

backend/adapters/market/ths_fund_flow_adapter.py
backend/adapters/market/ths_industry_constituents_adapter.py
-> hooks/sources/ths.py
-> sources/ths/client.py
-> sources/ths/parser.py
-> sources/ths/adapter.py
-> sources/ths/fund_flow.py
-> sources/ths/industry_constituents.py

backend/adapters/market/clickhouse_store.py
-> hooks/storage/clickhouse.py
-> storage/clickhouse/client.py
-> storage/clickhouse/readers.py
-> storage/clickhouse/writers.py
-> storage/clickhouse/ddl.py
-> storage/clickhouse/mutations.py

backend/adapters/market/duckdb_store/
-> hooks/storage/duckdb.py
-> storage/duckdb/client.py
-> storage/duckdb/readers.py
-> storage/duckdb/writers.py

拆分原则：

连接参数       -> connections/
连接创建       -> hooks/
外部请求       -> sources/*/client.py
原始解析       -> sources/*/parser.py
字段转换       -> sources/*/adapter.py
数据库读写     -> storage/
业务流程       -> pipelines/
job 入口       -> jobs/
4.2 services 迁移
backend/services/stock/
-> domain/stock/
-> domain/market/
-> domain/indicators/
-> pipelines/
-> jobs/

backend/services/scheduler/
-> jobs/
-> runtime/job_runner.py
-> runtime/job_store.py
-> runtime/checkpoints.py

backend/services/transcription_service.py
-> jobs/transcribe_media.py
-> pipelines/transcription/transcribe_media.py

只迁 执行逻辑，不迁 scheduler 配置状态。

比如老 scheduler 里可能包含：

什么时候跑
是否启用
上次状态
任务注册
任务 runtime 配置

这些不迁。
Python 只保留：

这个 job 被触发后具体干什么
执行成功/失败怎么返回
执行过程怎么写 job_run
checkpoint 怎么更新
4.3 repositories 迁移
backend/repositories/market/
-> storage/postgres/repositories/market/

backend/repositories/stock/
-> storage/postgres/repositories/stock/

backend/repositories/scheduler/
-> storage/postgres/repositories/job/
-> runtime/job_store.py

迁移判断：

数据生产需要的 repo    -> 迁
Java 需要读取的数据表   -> 迁
job_run/checkpoint repo -> 迁
UI 页面状态 repo        -> 不迁
AI 结果 repo            -> 不迁，除非只是 transcript 原文
旧 JSON 索引 repo       -> 不迁
4.4 db/config/scripts 迁移
backend/db/
-> storage/postgres/client.py
-> storage/postgres/ddl.py
-> storage/postgres/upserts.py

backend/config/
-> config/settings.py
-> config/connections.yaml
-> config/sources.yaml
-> config/database.yaml
-> config/jobs.yaml

backend/scripts/
-> scripts/init_clickhouse.py
-> scripts/init_postgres.py
-> scripts/backfill_kline.py
-> scripts/repair_trade_date.py
-> scripts/check_connections.py
-> scripts/check_data_quality.py

老脚本不能直接搬，必须改成调用新项目的 jobs 或 pipelines。

5. Job 体系设计
   5.1 JobRequest
   @dataclass
   class JobRequest:
   job_key: str
   trigger_request_id: str | None
   triggered_by: str | None
   dry_run: bool
   params: dict[str, Any]
   5.2 JobResult
   @dataclass
   class JobResult:
   run_id: str
   job_key: str
   success: bool
   status: str
   started_at: datetime
   finished_at: datetime | None
   rows_read: int = 0
   rows_written: int = 0
   warnings: list[str] = field(default_factory=list)
   errors: list[str] = field(default_factory=list)
   metadata: dict[str, Any] = field(default_factory=dict)
   5.3 JobRegistry
   JOB_REGISTRY = {
   "health_check": health_check.run,

   "sync_metadata": sync_metadata.run,
   "sync_trade_calendar": sync_trade_calendar.run,
   "sync_stock_basic": sync_stock_basic.run,

   "sync_kline_daily": sync_kline_daily.run,
   "sync_kline_minute": sync_kline_minute.run,
   "aggregate_kline_periods": aggregate_kline_periods.run,

   "sync_fund_flow": sync_fund_flow.run,
   "sync_margin": sync_margin.run,

   "build_msi": build_msi.run,
   "rebuild_msi": rebuild_msi.run,

   "backfill_history": backfill_history.run,
   "repair_missing_data": repair_missing_data.run,
   "quality_check": quality_check.run,

   # 只保留转文字，不做后续 AI
   "transcribe_media": transcribe_media.run,
   }
   5.4 JobRunner

runtime/job_runner.py 负责：

1. 校验 job_key
2. 创建 run_id
3. 创建 PipelineContext
4. 根据 trigger_request_id 做幂等
5. 获取 lock
6. 写 task_job_run started
7. 调用 job.run(context, request)
8. 捕获异常
9. 写 task_job_run success/failed
10. 释放 lock
11. 返回 JobResult

每个 job 自己不处理锁、不处理 job_run、不直接读 env。

6. Trigger API

Python 提供内部触发接口，不提供 UI API。

GET  /internal/health
GET  /internal/jobs
GET  /internal/jobs/{job_key}
POST /internal/jobs/{job_key}/trigger
GET  /internal/job-runs/{run_id}

请求：

{
"trigger_request_id": "java-unique-id-001",
"triggered_by": "java-scheduler",
"dry_run": false,
"params": {
"trade_date": "2026-06-28",
"codes": ["000001.SZ"],
"providers": ["eastmoney"],
"force": false
}
}

响应：

{
"accepted": true,
"run_id": "job_run_xxx",
"job_key": "sync_kline_daily",
"status": "success",
"result": {
"rows_read": 100,
"rows_written": 100,
"warnings": []
}
}

Java scheduler 的状态仍然在 Java。
Python 只记录本次执行 run。

7. 配置拆分
   7.1 connections.yaml

只放连接信息：

connections:
clickhouse_default:
type: clickhouse
host: ${CLICKHOUSE_HOST}
port: ${CLICKHOUSE_PORT}
database: ${CLICKHOUSE_DATABASE}
username: ${CLICKHOUSE_USERNAME}
password: ${CLICKHOUSE_PASSWORD}

postgres_default:
type: postgres
url: ${DATABASE_URL}

duckdb_default:
type: duckdb
path: ${DUCKDB_PATH}

eastmoney_default:
type: http
base_url: https://push2.eastmoney.com
timeout: 10
retry: 3

eastmoney_history:
type: http
base_url: https://push2his.eastmoney.com
timeout: 10
retry: 3

tencent_default:
type: http
base_url: https://web.ifzq.gtimg.cn
timeout: 10
retry: 3

sina_default:
type: http
base_url: https://money.finance.sina.com.cn
timeout: 10
retry: 3

ths_default:
type: http
base_url: https://data.10jqka.com.cn
timeout: 10
retry: 3

tdx_local:
type: filesystem
root_path: ${TDX_DATA_PATH}
7.2 sources.yaml

只放数据源策略：

sources:
kline:
daily:
providers: [tdx, eastmoney, tencent]
fallback: true
minute:
providers: [tdx, sina, eastmoney, tencent]
fallback: true

fund_flow:
stock:
providers: [eastmoney, ths]
fallback: true
sector:
providers: [ths, eastmoney]
fallback: true

stock_basic:
providers: [akshare, eastmoney, tencent]
fallback: true

trade_calendar:
providers: [tdx, akshare]
fallback: true

margin:
providers: [eastmoney, akshare]
fallback: true
7.3 jobs.yaml

只声明 Python 支持哪些 job，不放 cron，不放启用状态。

jobs:
sync_kline_daily:
description: 同步日线行情
params:
trade_date: optional
start_date: optional
end_date: optional
codes: optional
providers: optional
force: optional

sync_kline_minute:
description: 同步分钟行情
params:
trade_date: optional
codes: optional
providers: optional
force: optional

build_msi:
description: 构建 MSI
params:
trade_date: optional
force: optional

transcribe_media:
description: MP4/音频转文字
params:
input_uri: required
biz_id: optional
language: optional
force: optional
8. 迁移阶段
   Phase 0：Inventory

目标：不要盲迁，先盘点。

Agent 要生成：

docs/architecture/legacy_migration_inventory.md

表结构：

legacy_path | capability | new_path | migrate_action | status | notes

migrate_action 只能是：

migrate
split
rewrite
skip
review

重点盘点：

backend/adapters/market
backend/services/stock
backend/services/scheduler
backend/repositories
backend/db
backend/scripts
design/backend
Phase 1：底座

目标：先让新项目具备 job executor 能力。

实现：

connections/
hooks/
runtime/context.py
runtime/job_runner.py
runtime/job_store.py
runtime/locks.py
runtime/checkpoints.py
jobs/contracts.py
jobs/registry.py
jobs/health_check.py
interfaces/trigger_api/

验收：

python -m cynexus_data.jobs.health_check

curl http://localhost:8000/internal/health

curl -X POST http://localhost:8000/internal/jobs/health_check/trigger \
-H "Content-Type: application/json" \
-H "X-Cynexus-Internal-Token: <token>" \
-d '{"trigger_request_id":"test-001","triggered_by":"manual","dry_run":true,"params":{}}'
Phase 2：Storage 迁移

目标：先把 CH / DuckDB / PG 打通。

迁移：

backend/adapters/market/clickhouse_store.py
backend/adapters/market/duckdb_store/
backend/db/
backend/repositories/market/
backend/repositories/stock/
backend/repositories/scheduler/ 中 job_run/checkpoint 相关部分

新位置：

hooks/storage/clickhouse.py
hooks/storage/postgres.py
hooks/storage/duckdb.py

storage/clickhouse/
storage/postgres/
storage/duckdb/

验收：

python -m cynexus_data.jobs.health_check --check-storage

要求：

ClickHouse 可连接
DuckDB 可连接
Postgres 可连接
DDL 可初始化
job_run 可写入
checkpoint 可读写
Phase 3：Sources 迁移

目标：把老数据源拆成标准 source。

顺序：

1. EastMoney
2. TDX
3. Tencent
4. Sina
5. THS
6. AKShare

每个 source 统一拆成：

hook
client
parser
schema
adapter

验收：

pytest tests/unit/test_sources

要求：

source 不写数据库
source 不读 env
source 不管 job
source 不管 checkpoint
Phase 4：Datasets + Domain

目标：统一数据模型，把老 service 里的计算抽出来。

实现：

datasets/raw/
datasets/staging/
datasets/marts/

domain/stock/
domain/market/
domain/indicators/

优先迁：

K线模型
资金流模型
股票基础信息模型
交易日历模型
MSI 模型
K线聚合
复权
市场宽度
涨跌停情绪
资金流计算
波动率
换手率

验收：

pytest tests/unit/test_domain

要求：

domain 不访问外网
domain 不访问数据库
domain 不读 env
domain 不写文件
Phase 5：Pipelines 迁移

目标：把完整流程迁成 pipeline。

实现：

pipelines/kline/sync_daily.py
pipelines/kline/sync_minute.py
pipelines/kline/aggregate_periods.py
pipelines/kline/backfill.py
pipelines/kline/repair.py

pipelines/stock_basic/sync_stock_basic.py
pipelines/stock_basic/sync_trade_calendar.py

pipelines/fund_flow/sync_main_force.py
pipelines/fund_flow/sync_sector_flow.py
pipelines/fund_flow/sync_stock_flow.py

pipelines/margin/sync_margin.py

pipelines/msi/build_msi.py
pipelines/msi/rebuild_msi.py

pipelines/quality/run_quality_check.py

转写只保留：

pipelines/transcription/transcribe_media.py

不展开更多 AI 后处理 pipeline。

验收：

python -m cynexus_data.jobs.sync_kline_daily --dry-run
python -m cynexus_data.jobs.sync_kline_minute --dry-run
python -m cynexus_data.jobs.sync_fund_flow --dry-run
python -m cynexus_data.jobs.build_msi --dry-run
python -m cynexus_data.jobs.transcribe_media --dry-run --input-uri file:///tmp/demo.mp4
Phase 6：Jobs 迁移

目标：把老 Python “能执行的任务”迁成 job。

实现：

jobs/sync_metadata.py
jobs/sync_trade_calendar.py
jobs/sync_stock_basic.py
jobs/sync_kline_daily.py
jobs/sync_kline_minute.py
jobs/aggregate_kline_periods.py
jobs/sync_fund_flow.py
jobs/sync_margin.py
jobs/build_msi.py
jobs/rebuild_msi.py
jobs/backfill_history.py
jobs/repair_missing_data.py
jobs/quality_check.py
jobs/transcribe_media.py

每个 job 只做：

接收 JobRequest
调用 pipeline
返回 JobResult

不做：

管理 cron
管理启用状态
管理 Java scheduler 配置
拼 UI response
做 AI polish/summary/ask
Phase 7：Trigger API 对接 Java

目标：Java 能触发所有 Python job。

接口：

GET  /internal/health
GET  /internal/jobs
GET  /internal/jobs/{job_key}
POST /internal/jobs/{job_key}/trigger
GET  /internal/job-runs/{run_id}

验收示例：

curl -X POST http://localhost:8000/internal/jobs/sync_kline_daily/trigger \
-H "Content-Type: application/json" \
-H "X-Cynexus-Internal-Token: <token>" \
-d '{
"trigger_request_id": "java-sync-kline-daily-001",
"triggered_by": "java-scheduler",
"dry_run": true,
"params": {
"trade_date": "2026-06-28",
"codes": ["000001.SZ"],
"providers": ["eastmoney"]
}
}'

转写也是同一个接口：

curl -X POST http://localhost:8000/internal/jobs/transcribe_media/trigger \
-H "Content-Type: application/json" \
-H "X-Cynexus-Internal-Token: <token>" \
-d '{
"trigger_request_id": "java-transcribe-001",
"triggered_by": "java",
"dry_run": true,
"params": {
"input_uri": "file:///data/uploads/demo.mp4",
"biz_id": "media-001",
"language": "zh"
}
}'
9. 第一批落地任务

第一批不要迁全量，先打通整体骨架。

第一批范围
1. docs/architecture/legacy_migration_inventory.md
2. connections/
3. hooks/
4. runtime/context.py
5. runtime/job_runner.py
6. runtime/job_store.py
7. runtime/checkpoints.py
8. jobs/contracts.py
9. jobs/registry.py
10. jobs/health_check.py
11. interfaces/trigger_api/
12. storage/postgres/ job_run/checkpoint
13. storage/clickhouse/ 基础 client
14. storage/duckdb/ 基础 client
15. sources/eastmoney/ 基础拆分
16. pipelines/kline/sync_daily.py dry-run
17. jobs/sync_kline_daily.py
    第一批验收
    python -m cynexus_data.jobs.health_check
    python -m cynexus_data.jobs.sync_kline_daily --dry-run
    pytest tests/unit

HTTP：

curl http://localhost:8000/internal/health

curl http://localhost:8000/internal/jobs

curl -X POST http://localhost:8000/internal/jobs/sync_kline_daily/trigger \
-H "Content-Type: application/json" \
-H "X-Cynexus-Internal-Token: <token>" \
-d '{
"trigger_request_id": "manual-001",
"triggered_by": "manual",
"dry_run": true,
"params": {
"trade_date": "2026-06-28",
"codes": ["000001.SZ"],
"providers": ["eastmoney"]
}
}'

成功标准：

job trigger 能通
JobRunner 能创建 run_id
JobStore 能写 job_run
JobRegistry 能找到 job
PipelineContext 能创建 hooks
EastMoney source dry-run 能通
sync_kline_daily dry-run 能返回 JobResult
不依赖老 backend.*
10. 第二批落地任务

迁核心行情链路：

1. TDX
2. Tencent
3. Sina
4. K线分钟线
5. K线聚合
6. 历史回填
7. 缺失修复
8. K线质量检查

验收：

python -m cynexus_data.jobs.sync_kline_minute --dry-run
python -m cynexus_data.jobs.aggregate_kline_periods --dry-run
python -m cynexus_data.jobs.backfill_history --dry-run --start-date 2024-01-01 --end-date 2024-01-05
python -m cynexus_data.jobs.repair_missing_data --dry-run --dataset kline
11. 第三批落地任务

迁市场指标链路：

1. stock_basic
2. trade_calendar
3. fund_flow
4. sector_flow
5. industry/concept constituents
6. limit emotion
7. sector breadth
8. market pulse
9. market overview
10. MSI

验收：

python -m cynexus_data.jobs.sync_metadata --dry-run
python -m cynexus_data.jobs.sync_fund_flow --dry-run
python -m cynexus_data.jobs.build_msi --dry-run
python -m cynexus_data.jobs.quality_check --dry-run
12. 第四批落地任务

把非主链路能力补齐：

1. margin
2. 全量重算
3. 数据对账
4. audit
5. metrics
6. alert
7. transcribe_media job

transcribe_media 只做到：

MP4/音频 -> transcript
transcript 入库
返回 transcript_id

不做：

polish
summary
ask
embedding
AI provider 管理
13. 强制规则

Agent 执行时必须遵守：

1. 不允许整文件复制老代码。
2. 不允许新项目 import backend.*。
3. 不允许 source 写数据库。
4. 不允许 domain 访问外网或数据库。
5. 不允许 pipeline 直接读 env。
6. 不允许 storage 硬编码连接参数。
7. 不允许 Python 管理 Java scheduler 配置状态。
8. 不允许迁 AI provider。
9. 不允许迁 polish / summary / ask。
10. 不允许恢复旧 JSON runtime 方案。
11. 所有 job 必须走 JobRunner。
12. 所有 job 必须返回 JobResult。
13. 所有 pipeline 必须支持 dry_run。
14. 所有连接必须走 Connection + Hook。
14. 最终交付物

Agent 完成后必须输出：

1. 老文件 -> 新文件 mapping 表
2. 已迁移 source 列表
3. 已迁移 storage 列表
4. 已迁移 domain 列表
5. 已迁移 pipeline 列表
6. 已迁移 job 列表
7. trigger API 路由列表
8. Java 调用示例
9. 跳过项列表和原因
10. 测试结果
11. dry-run 结果
12. 未完成 TODO
15. 总结版

这次迁移应该按这个核心路径走：

老 backend/adapters
-> sources + hooks + storage

老 backend/services
-> domain + pipelines + jobs

老 backend/repositories
-> storage/postgres/repositories

老 scheduler 执行动作
-> jobs + runtime/job_runner

老 scheduler 状态配置
-> 不迁，Java 负责

老 AI provider / polish / summary / ask
-> 不迁，Java 负责

老 transcription
-> 只保留 transcribe_media job，不展开成主线

最重要的一句：

cynexus-data 不是“老 Python 后端重构版”，而是“Java 可触发的 Python Job Executor”。先把 job executor 的骨架跑通，再按数据源、storage、pipeline、job 分批迁移。