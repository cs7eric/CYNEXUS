# Findings

## 当前已知

- 用户要求小红书和抖音各自有 Job，帖子和评论不能再拆成两个 Job。
- 需要新建 Job categories，并沿用项目已有的 Job 管理方式。
- 需要记录评论采集耗时，至少能回答每次采集处理了多少评论、用了多久；最好额外提供每 1000 条评论的估算/耗时指标。
- 当前 `cynexus-data` 是 Python Job Executor，Java 负责 scheduler 状态和 UI API，Python 负责 job 执行与结果。
- 当前社媒 UI 手动触发逻辑仍调用 `social_creator_discovery` 和 `social_comment_sync` 两个 job。
- 当前社媒页面的目标配置和登录状态等改动是用户已有工作，不应覆盖。
- Scheduler 由 PostgreSQL migration seed `scheduler_jobs`、`scheduler_job_statuses` 和 `scheduler_job_category_mappings`；Job 的 Python key 通过 job `extra.data_job_key` 传给 data executor，`extra.default_params` 作为默认参数。
- 当前 scheduler 已有分类维护 API/UI；因此平台采集应新增两个分类和两个 scheduler job，而不是把平台塞进旧 `market_information` 分类。
- Python `JobRunner` 已持久化 started/finished 和总耗时；社媒 pipeline 的评论阶段耗时需要写入 `JobResult.metadata`，Java `SchedulerRunResultDomainPolicy` 需要把该指标带入 scheduler history 的 `result_summary.metrics`，UI 才能显示。
- 当前 `social_creator_monitor` 是 scheduled job 的目标来源；creator discovery 在传入 creators 时会 upsert 目标，未传入时会从该表读取启用目标。

## 待确认

- Java scheduler 的 category、job definition、trigger API 和 UI 配置所在模块。
- 社媒当前 discovery/comment pipeline 的可复用入口与写入结果结构。
- job run 结果是否已有 duration 字段，新增指标应落在哪一层。
- 是否需要将 UI localStorage 目标配置立即同步到 `social_creator_monitor`；当前合并 Job 可继续接收 UI 传入 creators，同时定时 Job 读取已落库目标。
