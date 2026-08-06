# 小红书 / 抖音博主新作品与评论日增量监控方案

## 1. 可行性结论

结论：**有条件可行，建议立项 POC，但不能把“全量评论、永久稳定、完全无人维护”写成承诺。**

在每个平台不超过 50 个博主、使用自有授权账号、只采集账号正常可见的公开作品与公开评论、每天增量运行、不购买第三方数据服务的前提下，Cynexus 现有基础设施足以承载：

- `cynexus-data` 已包含 Playwright 依赖；
- Data Executor 镜像已安装 Chromium；
- `/app/var` 已持久化，可保存隔离的浏览器登录态；
- Java Scheduler、DataJobExecutorClient、Python JobRunner、CheckpointStore、锁、运行记录和进度上报均已存在。

最大不确定性不是算力，而是平台页面、登录态、评论排序、账号风控和当前账号可见范围。因此项目目标应从“抓取平台全部评论”重写为：

> 在不绕过登录验证、验证码、权限和平台限制的前提下，按天发现指定博主的新作品，并以至少一次语义增量采集当前授权账号可见的公开评论与回复；通过唯一键去重、重叠窗口和滚动补采提高覆盖率。

## 2. 可行性评分

| 维度 | 评分 | 结论 |
| --- | ---: | --- |
| 100 个博主以内的规模 | 9/10 | 单机 Data Executor 足够，建议低并发分批执行 |
| 免费、自建 | 8/10 | 不需要第三方数据费，但仍占用现有机器、账号和维护时间 |
| 定时自动化 | 8/10 | Java Scheduler 与 Python JobRunner 可直接复用 |
| 新作品发现 | 7/10 | 使用 48 小时重叠窗口和作品 ID 去重后可达到较高覆盖 |
| 评论完整性 | 5/10 | 受热门排序、页面可见范围、折叠回复和平台限制影响，不能承诺绝对全量 |
| 长期稳定性 | 5/10 | 页面和内部响应结构会变化，需要监控、适配器测试与维护 |
| 综合 | 7/10 | 适合做受控 POC，按平台分别验收和上线 |

## 3. 重写后的业务需求

### 3.1 范围

- 小红书最多 50 个博主；抖音最多 50 个博主。
- 每个博主由用户录入平台主页标识或经过校验的主页地址。
- 只处理公开且登录账号正常可见的数据。
- 首期只做作品、一级评论、二级回复和基础统计，不做情感分析、用户画像或自动回复。

### 3.2 时效

- 新作品发现 SLA：发布后 24 小时内入库。
- 评论增量：每天至少完成一轮。
- 每轮扫描使用 48 小时作品发现重叠窗口，避免午夜边界、延迟展示和前一轮失败造成遗漏。
- 新作品进入 30 天滚动评论窗口；连续 7 天无新增后可降低优先级，但在 30 天内仍每天检查一次。

### 3.3 数据完整性口径

- 保证幂等：同一平台作品 ID、评论 ID、回复 ID 不重复入库。
- 保证可恢复：失败后从检查点继续，并对最近窗口重新扫描。
- 不保证平台全量：只承诺采集运行时账号可见且页面可加载的数据。
- 不根据“本轮没看到”直接判定评论删除；只有平台明确返回不可用状态时才更新可用性。

## 4. 推荐架构

```text
Java 管理端 / Domain
  ├─ 博主监控配置、启停、归属和权限
  ├─ Java Scheduler（日历日调度）
  └─ 作品/评论/运行状态查询 API
              │ DataJobExecutorClient
              ▼
Python Data Executor
  ├─ JobRunner / Lock / JobRun / Checkpoint
  ├─ social_creator_discovery
  ├─ social_comment_sync
  ├─ pipelines/social
  ├─ sources/douyin + sources/xiaohongshu
  ├─ hooks/sources/social_browser
  └─ storage/postgres/social
              │
              ▼
PostgreSQL
  ├─ social_creator_monitor
  ├─ social_post
  ├─ social_comment
  └─ 可选 social_collection_target_run

/app/var/social-browser-profiles
  ├─ douyin/<account-key>
  └─ xiaohongshu/<account-key>
```

### 4.1 为什么拆成两个 Job

1. `social_creator_discovery`：扫描博主主页，发现和更新作品。
2. `social_comment_sync`：扫描活跃作品，增量保存评论和回复。

拆分后，某个平台主页发现失败不会阻塞已入库作品的评论补采；评论任务超时也不会影响第二天的新作品发现。两个 Job 都应支持按 `platform`、`creator_ids`、`post_ids`、时间范围和最大页数进行有界重试与 dry-run。

### 4.2 浏览器运行方式

- 每个平台账号使用独立持久化 Chromium profile。
- 一个平台一次运行复用一个浏览器 context，不为每个博主重新启动 Chromium。
- 默认并发 1，验证稳定后最多 2；不要按 50 个博主并发打开页面。
- 通过页面正常加载产生的 DOM 或网络响应解析数据，不绕过验证码或访问控制。
- 初次登录或登录过期允许人工扫码；后续日常运行自动化。
- 登录过期、需要验证、访问受限必须识别成明确状态并告警，不能无限重试。

## 5. 增量算法

### 5.1 作品发现检查点

每个平台、每个博主保存：

```json
{
  "last_success_at": "2026-08-05T03:00:00+08:00",
  "latest_publish_at": "2026-08-04T21:30:00+08:00",
  "recent_post_ids": ["..."],
  "consecutive_failures": 0
}
```

每次查询区间从 `last_success_at - 48h` 开始。停止条件不是只看时间，而是连续遇到足够数量的已知旧作品，避免置顶作品和页面排序干扰。数据库唯一键负责最终去重。

### 5.2 评论检查点

每个平台、每个作品保存：

```json
{
  "last_success_at": "2026-08-05T05:00:00+08:00",
  "last_observed_comment_at": "2026-08-05T04:45:00+08:00",
  "resume_cursor": null,
  "empty_success_runs": 0,
  "active_until": "2026-09-04T00:00:00+08:00"
}
```

游标只作为单次大任务的续跑提示，不能作为唯一增量边界，因为平台游标可能过期。每天仍从前部重新扫描一段，并在连续遇到已知评论后停止。高评论量作品按页数或时间预算中断，下次从游标续跑，同时保留头部重扫。

二级回复只在以下情况补采：

- 一级评论首次出现；
- 平台显示的回复数大于已存回复数；
- 作品处于高优先级滚动窗口。

## 6. 调度建议

按亚洲/上海时区执行，两个平台错峰：

| 时间 | Job | 参数重点 |
| --- | --- | --- |
| 02:10 | 小红书作品发现 | `lookback_hours=48`, `max_creators=50` |
| 03:10 | 抖音作品发现 | `lookback_hours=48`, `max_creators=50` |
| 04:00 | 小红书评论增量 | `active_post_days=30`, `concurrency=1` |
| 06:00 | 抖音评论增量 | `active_post_days=30`, `concurrency=1` |

每个 Job 配置 `run_once_per_day=true`，但失败可由 Scheduler 有界重试。为减少固定访问模式，可在每个博主和作品之间加入小范围随机间隔；这只是负载平滑，不用于绕过平台验证。

如果评论任务超过每日窗口，应优先保证：新作品 > 最近 7 天作品 > 8～30 天作品，并将未完成队列保留到下一轮。

## 7. 数据模型

### 7.1 `social_creator_monitor`

- `id`, `tenant_id`, `owner_id`
- `platform`, `platform_creator_id`
- `profile_url`, `display_name`
- `enabled`, `last_discovered_at`, `status`, `status_reason`
- 唯一键：`tenant_id + platform + platform_creator_id`

### 7.2 `social_post`

- `id`, `tenant_id`, `creator_monitor_id`
- `platform`, `platform_post_id`, `post_type`
- `title`, `text_excerpt`, `published_at`, `canonical_url`
- `comment_count_observed`, `last_comment_sync_at`, `monitor_until`
- 唯一键：`tenant_id + platform + platform_post_id`

### 7.3 `social_comment`

- `id`, `tenant_id`, `post_id`
- `platform`, `platform_comment_id`, `parent_comment_id`
- `content`, `published_at`, `like_count`, `reply_count`
- `author_platform_id_hash`, `author_display_name`（可按产品需要关闭）
- `first_seen_at`, `last_seen_at`, `source_fetched_at`
- 唯一键：`tenant_id + platform + platform_comment_id`

MVP 使用 PostgreSQL 即可。当前规模无需同时引入 ClickHouse；只有在评论量长期达到千万级、需要复杂聚合分析时再做异步分析副本。

## 8. 失败与告警

失败必须归类，而不是统一记录为抓取失败：

- `LOGIN_REQUIRED`：登录失效，需要人工扫码。
- `VERIFICATION_REQUIRED`：平台要求额外验证，停止该账号任务。
- `ACCESS_RESTRICTED`：作品、博主或评论区当前不可访问。
- `PAGE_CONTRACT_CHANGED`：关键字段/节点/响应结构缺失。
- `RATE_LIMITED`：降低速度并结束本轮，不进行高频重试。
- `PARTIAL_SUCCESS`：部分博主或作品完成，检查点只更新成功目标。
- `TIME_BUDGET_EXCEEDED`：保留未完成目标，下一轮继续。

关键指标：目标博主数、成功博主数、新作品数、扫描作品数、读取评论数、写入评论数、重复数、失败分类、运行时长、最后成功时间、登录态年龄。

## 9. 分阶段实施

### Phase 0：平台 POC（必须先做）

- 每个平台选 5 个博主、20 条不同评论量级的作品。
- 完成一次授权登录和 14 天连续运行。
- 验证主页发现、一级评论、二级回复、检查点续跑和登录失效识别。
- 不先做完整 UI、Agent 工具或 ClickHouse。

### Phase 1：数据链路 MVP

- 新增平台 Connection、浏览器 Hook、Sources、两个 Pipelines、两个 Jobs。
- 新增 PostgreSQL 表和 storage repositories。
- 注册 jobs.yaml、JOB_REGISTRY 和 Java Scheduler 定义。
- 支持 dry-run、按平台/博主/作品有界执行。

### Phase 2：管理与可观测性

- 增加博主配置、启停、登录状态、运行状态和评论查询 API/UI。
- 增加告警、失败分类、目标级进度和人工重新登录流程。

### Phase 3：分析能力（可选）

- 情感、主题、关键词和舆情告警作为独立 AI 能力建设。
- 如需要 Agent 查询，只开放一个有日期/博主/行数限制的只读工具；不开放采集触发、任意 URL 或原始存储访问。

## 10. POC 验收与退出条件

建议两个平台分别验收，不要求一起通过：

- 连续运行 14 天，日任务成功率不低于 90%。
- 人工抽样的新作品 95% 在 24 小时内入库。
- 20 条抽样作品中，采集到的“账号页面可见评论”覆盖率不低于 90%。
- 数据库唯一键下重复记录为 0。
- 任务失败后可从检查点恢复，不需要整个平台重跑。
- 登录失效在一个调度周期内识别并告警。
- 按 50 + 50 博主估算或实测，完整日批在 8 小时窗口内完成。

任一平台出现以下情况应暂停该平台上线：

- 需要持续绕过验证码或频繁人工干预；
- 14 天内页面契约反复变化，维护成本不可控；
- 可见评论覆盖率长期低于产品可接受值；
- 单账号、单固定 IP 在低并发下仍频繁受限。

## 11. 最终推荐

采用“**平台分开验收、作品发现与评论同步分离、PostgreSQL 单库、Java 调度、Python 执行、Playwright 持久登录、低并发滚动补采**”的方案。

不要从一开始建设全量舆情平台，也不要承诺全量评论。先用 5 + 5 博主跑满 14 天，验证实际页面可见覆盖率和登录稳定性；通过后再扩到每个平台 50 个博主。

## 12. 当前 POC 实现与运行方式

本次已完成 Python 数据层 POC，代码范围包括：

- `sources/social`：Playwright 浏览器客户端、平台 URL/评论解析器、登录态错误分类；
- `hooks/sources/social.py`：从 Connection 创建平台浏览器客户端；
- `storage/postgres/social.py`：博主、作品、评论的幂等 upsert；
- `pipelines/social`：作品发现和评论同步两个 pipeline；
- `jobs/social_creator_discovery.py`、`jobs/social_comment_sync.py`：JobRunner 兼容入口；
- `connections.yaml`、`jobs.yaml`、`JOB_REGISTRY` 和 POC PostgreSQL DDL；
- 解析器、dry-run、JobRegistry 和 JobRunner 聚焦测试。

代码级验证命令：

```powershell
$env:PYTHONPATH = 'src'
python -m cynexus_data.jobs.social_creator_discovery --dry-run --platform xiaohongshu --creators '[{"creator_key":"demo-xhs","profile_url":"https://example.test/xhs"}]'
python -m cynexus_data.jobs.social_comment_sync --dry-run --platform douyin --posts '[{"post_key":"demo-douyin","url":"https://example.test/video/1"}]' --max-posts 1
pytest -q tests/unit/test_sources/test_social_parser.py tests/unit/test_pipelines/test_social_pipelines.py tests/unit/test_jobs_registry.py tests/unit/test_connections/test_registry.py tests/unit/test_runtime/test_job_runner.py
```

真实采集还需要：

1. 在 `var/social-browser-profiles/<platform>/<profile-name>` 准备对应平台的授权浏览器 profile；
2. 提供真实博主主页链接和稳定的平台 creator key；
3. 先运行作品发现，再运行评论同步；
4. 根据真实页面 DOM 调整 `connections.yaml` 中的帖子/评论选择器。

当前 POC 不包含 Java 管理 API、UI 登录引导、正式 Scheduler migration、Agent tool 和 AI 分析，这些应在真实页面抓取验证通过后再追加。
