# Agent Prompt Cache 企业级架构与实现指南

## 1. 文档目标

本文说明 Cynexus Agent 如何使用模型厂商的 Prompt Cache、为什么历史请求曾长期只命中约 `6.7K` Token，以及 v3 实现如何通过会话分区路由、Responses 状态链、全量历史回退和逐轮遥测提高缓存利用率。

本文讨论的是模型输入缓存，不是直接复用最终答案的响应缓存。Agent 仍会在每轮调用模型、执行权限校验并按需要调用 Tool。

## 2. 三类缓存不要混淆

| 类型 | 是否调用模型 | 缓存对象 | 适合解决的问题 |
| --- | --- | --- | --- |
| Provider Prompt Cache | 是 | 模型输入前缀的内部计算状态 | 降低重复输入的 Prefill 成本和延迟 |
| 应用响应缓存 | 否 | 完整答案 | 完全相同且允许复用的低时效请求 |
| Tool/数据缓存 | 不一定 | 行情、知识库或外部服务结果 | 降低外部查询频率 |

Prompt Cache 命中不表示输入文本没有发送，也不表示历史 Token 免费。`input_tokens` 通常仍包含完整计费上下文，`cached_input_tokens` 表示其中以缓存价格处理的部分。具体折扣、最小缓存长度、TTL 和模型支持范围由 Provider 决定。

## 3. 固定 6.7K 命中的诊断

历史四轮运行累计 `19,968` 个缓存读取 Token，恰好等于 `6,656 x 3`：第一轮未命中，之后三轮每轮都只命中同一个 `6,656` Token 前缀。这证明缓存并非完全失效，而是只复用了稳定 System Prompt 和 Tool 定义，新增会话历史没有逐轮进入可复用范围。

旧实现存在三个限制：

1. 所有会话共享 `stablePrefixKey`，兼容网关可能把大量不同会话后缀路由到同一缓存分区。
2. Responses 请求默认 `store=false`，也没有保存或发送 `previous_response_id`。
3. 本地只重建可见文本、Tool Call 和 Tool Result，没有重放 Provider 返回的完整 Responses output、Reasoning Item 或内部状态。

因此固定 `6.7K` 更像“静态前缀命中、增长历史未命中”，而不是 Provider 设置了 6.7K 上限。

## 4. v3 总体方案

```text
稳定 System Prompt + 排序后的 Tool Schema
                |
                v
       stablePrefixKey（全局诊断身份）
                |
会话摘要 --------+--------> routingKey（发送给 Provider）
                              |
                              v
第一轮：完整历史 + store=true
                              |
                    provider response id
                              |
后续轮：previous_response_id + 本轮增量
                              |
       状态字段不兼容/失效 ----+----> 完整历史无状态回退
```

实现遵循两个原则：缓存路由按会话隔离，Provider 状态只有在 Provider、Model 和稳定前缀签名完全一致时才复用。

## 5. PromptCacheContext v4

领域对象包含四个字段：

```java
PromptCacheContext {
    String conversationKey;
    String stablePrefixKey;
    String routingKey;
    String stablePrefixVersion;
}
```

### 5.1 conversationKey

`conversationKey` 是 `ownerKey + conversationNo` 的 SHA-256 摘要。原始租户、用户和会话标识不会发送给 Provider。

### 5.2 stablePrefixKey

`stablePrefixKey` 对以下内容做规范化摘要：

- Cache Key 算法版本；
- capability：`autonomous_agent`；
- Provider 和 Model；
- DB 管理的稳定 System Prompt；
- 按 Tool Code 排序后的 description、guidance、input/output Schema。

Prompt 或 Tool 定义变化会自动生成新签名，旧 Responses 状态不会继续复用。

### 5.3 routingKey

`routingKey` 将 `conversationKey` 与 `stablePrefixKey` 再次摘要。OpenAI、xAI 和 Generic 策略发送的 `prompt_cache_key` 都使用该字段。这样相同 Tool 前缀仍可被识别，但不同会话不会竞争同一个路由热点。

`prompt_cache_key` 是路由提示，不会让不同输入前缀变成相同前缀。真正命中仍要求输入前缀完全一致或 Provider 能根据状态链恢复相同上下文。

### 5.4 版本失效

当前 Key 版本为 `v4`。修改序列化规则、安全隔离边界或稳定前缀组成时必须升级版本，避免把语义不同的请求路由到旧缓存。

## 6. Responses 状态链

### 6.1 首轮请求

启用 `responses_state_mode=previous_response_id` 且 `disable_response_storage=false` 后，首轮发送完整历史并设置：

```json
{
  "store": true,
  "prompt_cache_key": "cynexus-agent-...",
  "input": ["稳定系统提示", "历史消息", "本轮用户消息", "运行时上下文"]
}
```

首轮没有 `previous_response_id`。成功后，Adapter 从普通响应 `id` 或流式 `response.completed.response.id` 获取 Provider Response ID。

### 6.2 后续 Tool 轮

后续请求发送：

```json
{
  "store": true,
  "previous_response_id": "resp_...",
  "input": ["本轮新增的 function_call_output", "新的运行时上下文"]
}
```

模型上一轮输出已经由 `previous_response_id` 引用，不再把同一 Assistant Tool Call 重复放进增量 input。领域请求同时保留：

- `messages`：完整可重放历史；
- `continuationMessages`：上一个 Provider Response 之后新增的用户消息或 Tool Result；
- `previousResponseId`：状态链锚点。

双轨数据是兼容性回退的基础，不是两份都发送。

### 6.3 跨 Run 继续

每次成功响应都把以下状态写入 `ai_agent_conversation`：

- `provider_response_id`；
- `provider_state_provider_code`；
- `provider_state_model_code`；
- `provider_state_signature`；
- `provider_state_updated_at`。

新 Run 只有在 Provider、Model 和 `routingKey` 全部一致时才复用 Response ID。切换模型、修改 Prompt、变更 Tool Schema 或升级 Cache Key 版本都会自然失效。

### 6.4 状态链不等于免计费

OpenAI 官方说明，使用 `previous_response_id` 时，链中此前输入仍会作为输入 Token 计费。状态链的价值是协议级连续性、避免错误重建完整 output，并为 Provider 的缓存和内部状态复用提供稳定路径；不能把它描述成“历史 Token 不再计费”。

## 7. 全量历史回退

OpenAI-compatible 网关不一定实现 Responses 存储。当状态续接请求返回 HTTP `400`、`404`、`409` 或 `422` 时，Adapter 会立即用同一领域请求中的完整 `messages` 重试，并移除 `previous_response_id`。

回退具有以下边界：

- 只对状态续接不兼容类错误触发；
- `5xx`、网络断开和限流仍由既有重试策略处理；
- 回退成功后保存新的 Response ID，后续轮可以重新尝试状态链；
- 遥测中的 `continuationMode` 标记为 `STATELESS_FALLBACK`；
- 流式状态回退当前通过非流式完整请求完成，最终答案仍会返回，但不会伪造已丢失的流式 Delta。

当模型返回文本形式的伪 Tool Call、领域层需要合成为结构化 Tool Call 时，该输出不是 Provider 原生 `function_call`。此时会主动清空 Response ID，下一轮使用完整历史，防止把 Tool Result 绑定到 Provider 不认识的 Call ID。

## 8. Provider 适配策略

| 策略 | 路由 | Usage 解析 |
| --- | --- | --- |
| OpenAI | Responses/Chat Body 的 `prompt_cache_key=routingKey` | `prompt_tokens_details`、`input_tokens_details` |
| xAI/Grok | Responses 使用 `prompt_cache_key`；Chat 可使用会话 Header | OpenAI 风格及 xAI 别名 |
| Generic | 对 OpenAI-compatible 请求发送 `prompt_cache_key=routingKey` | 常见 OpenAI-compatible 字段 |

状态链由 Responses Adapter 和模型绑定参数控制，缓存策略只负责 Provider 缓存路由与 usage 字段差异。这样 Domain 不依赖具体 HTTP 字段，同时未知兼容网关仍可使用统一能力并通过全量历史回退保持正确性。

## 9. 逐轮 CACHE_USAGE 遥测

Run 级累计值无法区分“每轮固定命中 6.7K”和“命中随历史增长”。v3 在 `ai_agent_run_event.details_json` 中为每次模型调用写一条 `CACHE_USAGE` 事件。

示例：

```json
{
  "turn": 2,
  "inputTokens": 19618,
  "cachedInputTokens": 11776,
  "cacheWriteTokens": 0,
  "cacheHitRatio": 0.6003,
  "cacheKeyHash": "sha256...",
  "stablePrefixHash": "sha256...",
  "requestInputHash": "sha256...",
  "inputItemCount": 3,
  "continuationMode": "STATEFUL",
  "responseStorageEnabled": true,
  "stateFallback": false,
  "previousResponseIdHash": "sha256...",
  "providerResponseIdHash": "sha256..."
}
```

事件不保存原始 Prompt、用户标识、会话号、Tool Result、API Key 或原始 Response ID。请求 input、Cache Key 和 Response ID 只记录 SHA-256，用于判断两轮请求身份是否稳定，不能用于恢复原文。

## 10. 排障方法

### 10.1 查看最近逐轮命中

```sql
SELECT run_no,
       sequence_no,
       details_json ->> 'turn' AS turn,
       details_json ->> 'continuationMode' AS mode,
       (details_json ->> 'inputTokens')::integer AS input_tokens,
       (details_json ->> 'cachedInputTokens')::integer AS cached_tokens,
       round(100 * (details_json ->> 'cacheHitRatio')::numeric, 2) AS hit_percent,
       details_json ->> 'stateFallback' AS state_fallback
FROM cynexus_appl_ai.ai_agent_run_event
WHERE type = 'CACHE_USAGE'
ORDER BY occurred_at DESC
LIMIT 50;
```

### 10.2 检查绑定是否启用

```sql
SELECT capability, provider_code, model_override, params
FROM cynexus_appl_ai.ai_usage_bindings
WHERE capability = 'autonomous_agent';
```

期望参数至少包含：

```json
{
  "wire_api": "responses",
  "responses_state_mode": "previous_response_id",
  "disable_response_storage": false,
  "prompt_cache_retention": "24h"
}
```

### 10.3 检查会话状态

```sql
SELECT conversation_no,
       provider_state_provider_code,
       provider_state_model_code,
       provider_state_signature IS NOT NULL AS has_signature,
       provider_response_id IS NOT NULL AS has_response_state,
       provider_state_updated_at
FROM cynexus_appl_ai.ai_agent_conversation
ORDER BY updated_at DESC
LIMIT 20;
```

不要把原始 `provider_response_id` 输出到普通应用日志或前端。

## 11. 如何判断优化有效

同一 Run 连续 Tool 轮的预期趋势是：

1. 第一轮通常是 Cache Miss 或只命中共享静态前缀；
2. 第二轮开始出现 `STATEFUL`，并携带前一轮 Response ID 哈希；
3. `cachedInputTokens` 不再机械地固定在 `6,656`；
4. 历史足够长且 Provider 支持时，缓存读 Token 应随可复用历史增长；
5. 如果一直是 `STATELESS_FALLBACK`，说明网关不支持或未透传 Responses 状态字段；
6. 如果 Input Hash 每轮异常变化，要检查 System Prompt、Tool Schema、工具排序和动态时间内容。

Codex 级别的大缓存数字通常来自非常长且稳定的系统指令、工具定义、工作区上下文和会话历史。应用无法通过一个 Key 凭空制造同样的命中规模；稳定前缀本身必须足够大、足够一致，Provider 也必须支持对应模型和路由。

## 12. 安全和数据治理

- Provider Response ID 是外部协议状态，存放在会话专用列，不暴露到 Agent API DTO。
- Cache telemetry 只存摘要和计数，并继续经过 `AgentSensitiveDataPolicy.redactJson`。
- Tool 权限、租户隔离和实时数据查询不会因 Prompt Cache 被跳过。
- 状态签名包含 Provider、Model、Prompt 和 Tool 协议，避免跨模型或跨 Schema 误续接。
- Response ID 失效只影响性能，完整历史回退保证业务正确性。
- 数据保留时间还应结合 Provider 合同、组织合规要求和本地会话清理策略设置。

## 13. 测试清单

- Tool 顺序变化不改变 `stablePrefixKey`；
- 会话变化会改变 `routingKey`，但相同 Prompt/Tool 保持同一 `stablePrefixKey`；
- System Prompt 或 Tool Schema 变化同时改变稳定签名和路由 Key；
- 首轮 Responses 请求 `store=true` 且不含 `previous_response_id`；
- 后续请求仅发送增量 Tool Result 和运行时上下文；
- 普通和流式响应都解析 Provider Response ID；
- 状态错误会触发完整历史回退并记录 `STATELESS_FALLBACK`；
- 对话状态按 Provider、Model 和签名持久化；
- `CACHE_USAGE` 不包含原始会话或 Response ID。

## 14. 发布、回滚和降级

发布顺序：

1. 执行 `20260726_add_ai_agent_response_state_and_cache_telemetry.sql`；
2. 执行 v3 中文知识库迁移并重建文档 Chunk/索引任务；
3. 发布 AI Release 镜像；
4. 检查绑定参数、数据库列和服务健康状态；
5. 运行多轮 Tool 场景并检查逐轮 `CACHE_USAGE`。

需要紧急降级时，可把绑定中的 `responses_state_mode` 删除，或设置 `disable_response_storage=true`。代码会回到完整历史请求，Response ID 不会被复用；已有状态列可以保留，不影响旧版本。若某网关拒绝 `prompt_cache_retention`，先从该绑定 params 中删除该字段，不需要回滚数据库结构。

## 15. 参考资料

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- OpenAI Conversation State：<https://developers.openai.com/api/docs/guides/conversation-state>
- xAI Prompt Caching：<https://docs.x.ai/developers/advanced-api-usage/prompt-caching>
- Anthropic Prompt Caching：<https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching>
- Gemini Context Caching：<https://ai.google.dev/gemini-api/docs/caching>
- vLLM Automatic Prefix Caching：<https://docs.vllm.ai/en/latest/features/automatic_prefix_caching/>
