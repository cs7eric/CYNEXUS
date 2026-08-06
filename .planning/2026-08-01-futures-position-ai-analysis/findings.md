# 调研记录

## 首轮发现

- 现有可复用的完整 AI 业务案例是 `market_information_event_analysis` / `market_information_daily_brief`，实现位于 `cynexus-ai` 的 `MarketInformationAiDomainService`、`MarketInformationAiCompletionAdapter` 与 `MarketInformationAiWorker`。
- Prompt 解析遵循 binding `prompt_id` -> `ai_prompt`，缺失时由 domain fallback；生产 prompt 必须通过 `deploy/local/postgres/migrations` seed。
- AI Provider 页面从后端 capability catalog 读取 routing 元数据，前端服务入口在 `cynexus-ui/src/services/ai/provider.ts`，页面在 `cynexus-ui/src/views/ai/provider/index.tsx`。
- 期货持仓当前已有查询 API 和历史结构数据，但没有 AI 分析 API/服务；页面只生成本地规则型“数据解读”。
- 当前任务需要新增一个期货专用 capability，并把可用范围限制为 IF、IH、IM、IC，避免对其他品种开放。

## 实现决策

- 新 capability 使用 `market_futures_position_analysis`，由内置 `AgentToolHandler` 注册，因此会进入 AI Provider 页的 Capability routing，并保留 runtime entry、prompt key、group、description 等元数据。
- 交互入口走 `/api/ai/futures-position/analysis`，前端把当前快照、历史序列和会员排名作为输入；domain service 负责品种白名单、输入截断、binding/prompt 解析、模型路由、JSON 校验和 execution log。
- AI 输出只保留事实型字段：`headline`、`structure`、`evidence`、`trend`、`risk`、`watch`、`data_quality`、`disclaimer`，不生成分数或买卖方向。
- Prompt 初始 ID 与现有数据冲突，已调整为未占用的 `910003000000000305`；usage binding 使用 `910002000000000011`，并验证迁移可重复执行。
- 当前数据库已验证：`market_futures_position_analysis` prompt 为 `v1/active`，binding 启用并绑定 `custom-yancc` 与对应 `prompt_id`；Agent capability 需要新 AI 镜像启动后由 handler 注册。
- Docker 容器内 `host.docker.internal` 优先解析 IPv6，Java 代理请求会出现 `ClosedChannelException`；本次 Release 验证使用 Docker Desktop IPv4 网关 `192.168.65.254`，容器正常启动并完成 capability 注册。
- 真实 Provider 响应已到达上游，当前账号的 `custom-yancc` 分组无权访问；数据库最终已恢复 migration 预设的 `custom-yancc/auto` binding。
- 非白名单输入原先会被默认异常处理返回 500，已在期货 AI controller 映射为 HTTP 400，避免前端把参数错误误认为服务故障。
- Provider 命名确认：数据库中 `code=custom`、`name=xtyooo`、`base_url=https://am.xtyooo.top`，因此 migration 与当前 binding 使用 `custom`，不是 `custom-yancc`。
- 期货分析 API 现在将 Provider 异常转换为 `Result.fail("AI_PROVIDER_ERROR", ...)`，前端可显示上游错误；当前 xtyooo key 返回 `GROUP_DISABLED`，该问题属于 Provider 账号配置，不是页面请求参数或 capability 链路。
