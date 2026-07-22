# Agent Phase 107 动态 Capability Release 验证

日期：2026-07-22（Asia/Shanghai）

## 结论

Release 运行验证通过了“Capability 注册后，真实模型自主选择并调用非 market capability”这一条路径：`knowledge.search` 在 Release 重启时由已装配的 handler descriptor 重新注册，随后真实 `gpt-5.4-mini` 自主选择并成功调用了 `knowledge.search` 和 `knowledge.document`。

但这不是进程不停机的 hot-add 验证。当前代码没有 capability 注册 API；本次使用“删除持久化 descriptor 后，通过 Release 启动同步重新注册”的方式验证动态发现。若 Phase 107 的“动态新增”要求运行中通过 Provider 页面/API 即时新增一个全新 runtime capability，则该项仍未完成，原因是没有对应注册入口，且新增 Java handler 会违反本次“不要修改 Java/UI 代码”的范围。

## Runtime

- 独立 worktree：`E:\project-repo\cynexus-agent-dynamic-capability`
- Branch：`codex/agent-dynamic-capability-release-20260722`
- Maven development version：`1.1.177-SNAPSHOT`
- Release image：`1.1.177-260722-RELEASE`
- 启动入口：`deploy/release/start-release.ps1 -Version 1.1.177-260722-RELEASE -Build`
- Release 只运行版本化 Docker 镜像；Maven reactor 41/41 成功，Core、AI、Market、Knowledge Base、Gateway、UI 六个容器均启动。
- 健康检查：`18080`、`18081`、`18082`、`18084`、`18000` 返回 HTTP 200，UI `5173` 返回 HTTP 200。
- 发现一个遗留的无宿主端口应用副本 `phase107-ai-2`，为遵守“只允许一个应用环境在线”规则已停止；共享 PostgreSQL、Redis、ClickHouse、Nacos、Data Executor 未停止。

## Dynamic registration evidence

验证对象选择非 market capability：`knowledge.search`。

1. Release 重启前，Capability API 返回该 row，元数据为 `version=1.0.0`、`module=knowledge-base`、`runtimeEntry=cn.cccs7.cynexus.ai.infra.agent.knowledge.KnowledgeSearchAgentToolHandler`、`promptKey=autonomous_agent`、`routeGroup=Agent / knowledge-base`、`source=Knowledge Base API`、`freshness=published knowledge documents`、`enabled=true`、`availability=AVAILABLE`。
2. 从 `cynexus_appl_ai.ai_agent_capability` 删除 `knowledge.search` 后，Capability API 不再返回该 row；数据库命令结果为 `DELETE 1`。这一步仅用于制造“未注册”状态，不修改 Java/UI 源码。
3. 通过 Release Docker 入口重建并启动后，Capability API 再次返回该 row，新的数据库 id 为 `1784733870849`，`lastSeenAt=2026-07-22T15:25:29.4887Z`。
4. `ai_agent_capability_audit` 新增：`REGISTERED|knowledge.search|1.0.0|enabled=true|availability=AVAILABLE|Handler descriptor synchronized|2026-07-22 15:24:39.858758+00`。
5. Release Capability API 最终返回 14 个 enabled/available capability；该新增注册项保持非 market 的 Knowledge Base runtime metadata，未通过固定关键词或 executor 分支注入。

## Real model request

请求地址：`POST http://127.0.0.1:18081/api/ai/agent/runs`

使用受信任身份请求头 `X-Cynexus-Principal`、`X-Cynexus-Tenant`、`X-Cynexus-Identity-Signature`；签名值未写入文档。

请求体：

```json
{"userInput":"请查找并概括已发布内部资料《A Stock 数据落库表设计》的要点，列出相关文档标题、状态和关键结论。","idempotencyKey":"phase107-dynamic-knowledge-20260722","runNo":"AGR-107-dynamic-knowledge-20260722","locale":"zh-CN","workspace":"default"}
```

运行结果：

- `runNo=AGR-107-dynamic-knowledge-20260722`
- `ownerKey=default::workspace-user`
- `status=SUCCEEDED`
- `modelCode=gpt-5.4-mini`
- `turnCount=3`
- `toolCallCount=2`
- `usageSource=REPORTED`
- `inputTokens=26316`、`outputTokens=813`、`totalTokens=27129`
- `traceId=c4127c7f6e1a44cd8bc26e45e1e2bdd9`

PostgreSQL `ai_agent_run_step` 证据：

| step | kind | tool | status | input | summary |
| ---: | --- | --- | --- | --- | --- |
| 1 | TOOL | `knowledge.search` | `SUCCEEDED` | `{"query":"A Stock 数据落库表设计","limit":10}` | Found 10 published knowledge documents. |
| 2 | TOOL | `knowledge.document` | `SUCCEEDED` | `{"documentId":332571765114957824}` | Read the knowledge-base document. |
| 3 | ANSWER | - | `SUCCEEDED` | `{}` | Generated the final Agent answer. |

事件序列为：`RUN_STARTED`、`RETRYING`、`TOOL_STEP(knowledge.search)`、`TOOL_STEP(knowledge.document)`、`ANSWER`、`RUN_COMPLETED`。

Provider routing 的数据库证据为：`autonomous_agent` binding 指向 provider id `910001000000000005`，model override 为 `gpt-5.4-mini`；该 provider 的 `code=custom`、`provider_type=custom`、`base_url=https://am.xtyooo.top`、`default_model=gpt-5.4-mini`。本次响应 DTO 没有暴露 `providerCode` 字段，因此不把未持久化的字段宣称为 run provenance；模型、binding、provider 配置和 Release 镜像均单独记录。

## Failure evidence

第一次真实 Provider 请求发生超时，AI 日志记录：`AI provider custom agent failed for model gpt-5.4-mini at https://am.xtyooo.top/v1/chat/completions: HttpTimeoutException`。数据库事件序列也保留了 `RETRYING`，摘要为 `Retrying the AI provider request (attempt 2).`。第二次请求成功，未把第一次失败伪装成成功，也未生成 mock/synthetic 结果。

本次未观察到 `MockAiModelAdapter`、`synthetic` 或 `fabricated` 执行证据；Release AI 使用版本化镜像 `cynexus-ai:1.1.177-260722-RELEASE`。Provider 运行日志中有一次真实网络超时，因此本次验证结论是“真实模型路径通过且具备重试后的成功证据”，不是“Provider 网络稳定性通过”。

## Scope check

- Java/UI 源文件：未修改。
- Maven/UI 版本：未修改；本次只有文档验证，没有 functional source change，因此未做版本递增。
- 本次提交应只包含本文件：`docs/agent-dynamic-capability-release-20260722.md`。
