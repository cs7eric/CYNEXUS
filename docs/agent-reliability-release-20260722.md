# Cynexus Agent Phase 107 运行验证记录

日期：2026-07-22
范围：多实例/高并发 quota、task lease recovery、dead-letter、notification adapter
结论：未通过 Phase 107 完成门禁。本文只记录真实执行结果；未执行成功的项目明确标记为阻塞。

## 验证边界

- 独立工作区：`C:\Users\Cs7er\.codex\worktrees\7a5a\cynexus`。
- 工作区提交：`04abb131`（detached HEAD），Maven 与 UI 均为 `1.1.164-SNAPSHOT`。
- 验证前工作区干净；本次未修改 Java/UI，也未修改版本号。
- 验证开始时 Release 应用栈为版本化镜像 `1.1.177-260722-RELEASE`。该运行实例的 AI 日志显示实际 jar 为 `cynexus-ai-infra-1.1.178-SNAPSHOT.jar`，因此不能把它当作当前工作区 `1.1.164-SNAPSHOT` 的构建证据。
- 验证期间 Release 栈被外部重建为 `1.1.179-260722-RELEASE`；结束时 Core、Market、AI、Knowledge Base、Gateway、UI 健康检查均为 `UP`。共享 PostgreSQL、Redis、Nacos、ClickHouse 和 Data Executor 未停止。

## 聚焦测试

命令：

```powershell
mvn -pl cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller -am test `
  '-Dtest=AgentTaskAppServiceTest,AgentStaleRunRecoveryJobTest,AutonomousAgentWorkflowTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false'

mvn -pl cynexus-ai/cynexus-ai-infra -am test `
  '-Dtest=RedisBackedAgentQuotaPortTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false'
```

结果：

- 应用层 11/11 通过：`AgentStaleRunRecoveryJobTest` 1、`AgentTaskAppServiceTest` 2、`AutonomousAgentWorkflowTest` 8。
- quota infra 测试 1/1 通过。
- 这些测试包含 mock/单进程边界，不能替代下面的多实例 Release 证据。

## 多实例与高并发 quota

### 已执行

1. 在 Release 镜像 `cynexus-ai:1.1.177-260722-RELEASE` 上临时启动第二个 AI 容器。第一次尝试因容器退出码 137 失败；降低 JVM 内存后第二次启动成功。
2. Nacos 实际返回两个健康实例：`172.18.0.11:18081` 和 `172.18.0.16:18081`。第二实例完成注册后，通过 Gateway 执行 20 个并发 Agent 请求。
3. 为避免大量真实 Provider 调用，测试 owner 的当日 quota 行先保存，再临时设置为 `request_count=199`、`reserved_tokens=0`、`total_tokens=0`。测试结束后恢复为原始值：`request_count=9`、`reserved_tokens=0`、`input_tokens=133716`、`output_tokens=2527`、`total_tokens=136243`、`estimated_usage_count=2`。

### 观察结果

- 20 个并发请求全部到达 Gateway；quota 行从 `199` 增加到 `200`，`reserved_tokens` 最终为 `0`，说明共享数据库的 quota reservation 没有越过请求上限。
- 其中 1 个请求真正进入 Provider 调用并持久化为 `FAILED`；其余请求主要被共享 Redis active-run lease 拒绝，应用错误消息为 `Too many active Agent runs for this owner`。
- 该 Release 的 HTTP 响应外层仍为 HTTP 200，错误在 JSON `code=100003` 中表达；因此本次不能宣称真实运行时已经证明 HTTP 429 quota contract。HTTP 429 只在代码路径/聚焦测试预期中存在，未在本次并发探针中观察到。
- 第二实例随后被移除，Nacos 恢复为单实例；没有留下临时容器。

### 真实 Provider 阻塞

被 quota 放行的请求使用 `gpt-5.4-mini`，路由到 custom provider `https://am.xtyooo.top/v1/chat/completions`。AI 日志记录：

```text
SSLHandshakeException: Remote host terminated the handshake
```

因此没有成功的真实 Provider Agent run，也没有可用于 Phase 107 成功门禁的 Provider success、usage provenance 或 capability evidence。

## Task lease recovery

通过 Release Gateway 创建了任务 `ATK-69390054c969446b815b243b`，先将其排期到未来，再在 PostgreSQL 测试夹具中模拟进程崩溃：

- `status=RUNNING`
- `lease_owner=phase107-crashed-worker`
- `lease_until` 设为过去时间
- `next_attempt_at=now()`

随后实际 worker 将任务重新认领：

- `attempt_count` 从 `0` 变为 `1`。
- `lease_owner` 变为真实 worker id `agent-worker-7ccc9563-92a1-499f-8a30-89b22bc51832`。
- `lease_until` 被延长约 900 秒。

这一段证明了过期 task lease 的数据库 claim/recovery 路径确实运行。为避免测试任务继续占用 lease，最后通过正式 cancel API 将任务置为 `CANCELLED`。

## Dead-letter

未通过。该任务继续执行失败分支时，Release AI 日志出现真实数据库错误：

```text
ERROR: value too long for type character varying(1000)
SQL: UPDATE cynexus_appl_ai.ai_agent_task
     SET status = 'DEAD_LETTER', last_error = ?, ...
```

数据库现场确认 `cynexus_appl_ai.ai_agent_task.last_error` 为 `varchar(1000)`。由于实际错误文本超过列长度，`deadLetter` 更新回滚，任务没有进入 `DEAD_LETTER`；这不是成功证据。该阻塞需要后续代码/迁移修复后，重新用版本化 Release 镜像验证。

## Notification adapter

未发现可执行的后端 notification adapter：

- Agent domain/application/infra 下没有 notification port、adapter、outbox、delivery worker 或 notification runtime endpoint。
- 搜索结果只有 UI 的 `notification.tsx`/`alert.tsx`，不能证明后台任务完成、失败或 dead-letter 会产生通知。
- 因没有 adapter、配置和 Release API，无法进行真实发送、重试、失败或 dead-letter 通知验证。

该项明确阻塞，不能用 UI notification 组件替代后端 adapter 证据。

## 收尾状态与门禁结论

- 临时第二 AI 实例已移除；Release 栈最终为 `1.1.179-260722-RELEASE`，18000、18080、18081、18082、18084 健康检查均为 HTTP 200/`UP`。
- quota 测试夹具已恢复；测试任务保留为已取消的审计记录，不再持有 lease。
- 当前独立工作区最终仍只有本文件是本次新增内容；Java/UI 未改动。
- Phase 107 的 background task acceptance 仍为 **blocked**：lease recovery 可执行，但 dead-letter 持久化失败，notification adapter 不存在。
- 多实例 quota 只完成了共享 reservation/active lease 的部分证据；真实 Provider 被 TLS/代理阻塞，HTTP 429 runtime contract 未被观察到。因此不能将本次验证记为 Phase 107 完成。
