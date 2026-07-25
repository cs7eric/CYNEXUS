# Findings

## Requirements
- Agent 输入框必须能改变 reasoning effort。
- 进入页面时应显示当前 Provider 的默认 reasoning，而不是固定 default。
- 选择 Provider 时应同步其默认 reasoning。
- reasoning 选择应随 Agent 请求传到后端并影响实际模型调用。

## Research Findings
- `cynexus-ui/src/views/ai/agent/index.tsx` 当前只有 Provider、Model 选择，没有 reasoning 选择。
- Agent 初始化模型使用 `binding.model_override || provider.default_model || provider.models[0]`。
- Provider 页面把默认 reasoning 保存到 `provider.extra.model_reasoning_effort`。
- Agent 请求当前只有 `preferredProviderCode` 和 `preferredModelCode`。
- `OpenAiCompatibleChatAdapter` 已从 `AiModelBO.extra.model_reasoning_effort` 构造 Responses API reasoning 参数。
- 工作区已有大量未提交的前序修改，必须只在本任务相关文件上继续工作。

## Technical Decisions
| Decision | Rationale |
|---|---|
| 在 domain 的模型解析阶段复制并覆盖 `AiModelBO.extra` | 业务规则归 domain；避免修改数据库返回对象或 Provider 持久化默认值 |
| 保留原有两参数模型解析调用 | 降低现有场景和测试的回归风险 |

## Follow-up Findings
- `autonomous_agent` 当前 binding `params` 为 `{"prompt_version":"v2"}`，provider extra 为 `{"model_reasoning_effort":"medium"}`，但 binding params 未参与 Agent 模型解析。
- `AgentRunDomainService.resolveModel` 当前只读取 binding provider/model override，并直接复制 provider extra；reasoning 需要在此处完成 binding -> runtime model 的解析。
- Agent 的 `wire_api` 缺失时走 Chat Completions；现有 chat payload 未发送 reasoning，因此 Responses-only 支持不足以满足 binding 配置生效要求。
- reasoning 允许值和默认值应集中定义，避免 UI、domain、adapter 各自维护字符串常量。

## Resources
- `cynexus-ui/src/views/ai/agent/index.tsx`
- `cynexus-ui/src/services/ai/agent.ts`
- `cynexus-ai/cynexus-ai-api/src/main/java/cn/cccs7/cynexus/ai/api/request/AgentRunRequest.java`
- `cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller/src/main/java/cn/cccs7/cynexus/ai/application/service/agent/execution/AutonomousAgentAppService.java`
- `cynexus-ai/cynexus-ai-domain/src/main/java/cn/cccs7/cynexus/ai/domain/value-object/agent/AgentRunExecutionCommand.java`
- `cynexus-ai/cynexus-ai-domain/src/main/java/cn/cccs7/cynexus/ai/domain/service/agent/execution/AgentRunExecutionDomainService.java`
- `cynexus-ai/cynexus-ai-domain/src/main/java/cn/cccs7/cynexus/ai/domain/service/agent/execution/AgentRunDomainService.java`
