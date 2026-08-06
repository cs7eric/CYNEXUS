# 两融更新探针

## Goal

在两融正式同步前每 10 分钟探测数据源是否已经出现新的交易日数据；未更新时本轮只记录 skipped，不执行全量落库；更新后触发正式两融同步，并避免重复触发。

## Phases

- [in_progress] 梳理现有调度、数据源和运行状态契约
- [pending] 实现探针 Job、调度迁移和运行参数
- [pending] 增加测试并同步版本
- [pending] 编译、构建并通过 Release 栈验证

## Decisions

- 探针使用轻量的 EastMoney 两融单股历史接口，不抓全市场明细。
- 探针的调度间隔由现有 scheduler 的 `interval_minutes=10` 约定表达；需要兼容现有调度器字段。
- 探针检测到新交易日后触发正式 `sync_margin`，正式 Job 自身仍保留幂等能力。

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| `runtime/runner.py` 不存在 | 1 | 待按实际 runtime 文件定位 Job 执行入口 |
