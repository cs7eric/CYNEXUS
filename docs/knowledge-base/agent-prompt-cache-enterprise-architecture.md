# Agent Prompt Cache 企业级架构与实现指南

## 1. 文档目标

本文解释 Cynexus Agent 的 Prompt Cache 原理、历史请求为什么长期只命中约 `6.7K` Token，以及 v4 如何在 xhhai 当前无法使用 Responses WebSocket 和 HTTP `previous_response_id` 的条件下，通过 Responses HTTP 无状态原生回放提升缓存利用率。

本文讨论的是模型输入前缀缓存，不是直接复用最终答案的响应缓存。Agent 每一轮仍会调用模型、执行 Tool 权限校验、读取实时数据并生成新答案。

## 2. 先区分三类缓存

| 类型 | 是否调用模型 | 缓存对象 | 主要收益 |
| --- | --- | --- | --- |
| Provider Prompt Cache | 是 | 输入前缀的 KV/Prefill 计算状态 | 降低重复输入的成本和延迟 |
| 应用响应缓存 | 否 | 已生成的最终答案 | 完全相同、允许复用的低时效请求 |
| Tool/数据缓存 | 不一定 | 行情、知识库、外部服务结果 | 降低外部查询频率和延迟 |

Prompt Cache 命中不代表历史文本没有发送，也不代表全部输入 Token 免费。`input_tokens` 仍表示本轮完整输入规模，`cached_tokens` 表示其中从缓存读取的部分。折扣、TTL、写入费用和模型支持范围由 Provider 决定。

## 3. OpenAI 官方机制

### 3.1 精确前缀匹配

缓存命中要求输入从第一个 Token 开始形成精确相同的前缀。稳定内容应放在前面，动态内容放在后面：

```text
稳定 System Prompt
稳定且排序固定的 Tool Schema
稳定历史 input/output items
动态运行时上下文
当前 Tool Result 或用户输入
```

消息、图片、Tool 定义和 Structured Output Schema 都会参与前缀匹配。Tool 顺序、description、Schema 字段顺序或动态时间内容提前出现，都可能缩短可命中前缀。

### 3.2 自动缓存和最小长度

近期 OpenAI 模型会自动启用 Prompt Cache。输入达到 `1,024` Token 后才具备缓存资格；低于该值时 Usage 仍会返回 `cached_tokens`，但值为 `0`。

### 3.3 prompt_cache_key 的作用

`prompt_cache_key` 影响请求路由，使共享长前缀的请求更可能进入同一缓存节点，但它不会把不同 Prompt 变成相同 Prompt，也不会绕过精确前缀匹配。

OpenAI 建议单个 Key 的总流量约保持在每分钟 `15` 个请求以内。高并发场景应使用稳定分片，同一会话始终落入相同分片。

### 3.4 Retention 差异

- GPT-5.6 及后续模型使用 `prompt_cache_options`，可以使用显式 breakpoint；当前 TTL 最小值为 `30m`。
- 较早模型使用 `prompt_cache_retention`。支持扩展保留的模型可以配置 `24h`。
- 兼容网关不一定支持所有字段；Provider 拒绝某字段时必须按能力降级，不能假设模型名称相似就完全兼容。

### 3.5 store=false 不会关闭 Prompt Cache

`store=false` 只控制 Provider 是否持久化 Response 对象，不会关闭 Prompt Cache。无状态模式仍可缓存输入前缀，但客户端必须自己保存并回放会话所需的原生输出项。

## 4. 固定 6.7K 命中的根因

历史四轮数据为：

```text
input : 15163, 25959, 27375, 38993
cached:     0,  6656,  6656,  6656
```

第一轮未命中，之后三轮都只命中 `6,656` Token。这说明缓存并非完全失效，而是只复用了稳定 System Prompt 和 Tool Schema；不断增长的会话历史没有形成可复用的 Provider 原生前缀。

旧实现只保存了可见 Assistant 文本、解析后的 Tool Call 和 Tool Result，丢失了 Responses `output[]` 中的以下内容：

- `reasoning` 及其 `encrypted_content`；
- Provider 原生 `function_call` item；
- Assistant message 的 `phase`；
- 其他未来可能出现的 opaque output item；
- 产生该输出前紧邻的动态运行时上下文。

下一轮虽然看起来拥有相同“聊天内容”，但在线路协议上已不是上一轮完整 output 的延续，因此只能命中最前面的静态区域。

## 5. xhhai 能力验证

2026-07-26 对 `https://api.xhhai.online` 的实际探测结果：

| 能力 | 结果 | 处理方式 |
| --- | --- | --- |
| HTTP `/v1/responses` | 成功 | 作为当前正式传输 |
| Responses WebSocket Upgrade | 成功建立后关闭 `1013 no available account` | 当前禁用，保留 Adapter 能力 |
| HTTP `previous_response_id` | 返回 `400`，提示仅支持 WebSocket v2 | xhhai 绑定不发送该字段 |
| `store=false` 完整 `output[]` 回放 | 成功 | 当前会话连续性方案 |
| reasoning + function_call + function_call_output 回放 | 成功 | Agent Tool 链采用该方式 |

OpenAI 官方服务支持 HTTP 和 WebSocket 的 `previous_response_id`。xhhai 的限制属于 Provider 兼容差异，不应写成 Responses API 的通用限制。

## 6. v4 总体方案

```text
稳定 System Prompt + 排序后的 Tool Schema
                    |
                    v
             stablePrefixKey
                    |
       conversation hash -> 16 个稳定路由分片
                    |
                    v
        prompt_cache_key (v5 routing key)

Responses HTTP, store=false
  用户输入 + runtime-1
       -> output-1[]
  原样回放 output-1[] + tool-result-1 + runtime-2
       -> output-2[]
  原样回放 output-2[] + tool-result-2 + runtime-3
       -> output-3[]
```

每轮请求都携带完整无状态 input，但前缀从稳定 Prompt、Tool、历史 input/output 开始保持一致。Provider 可以复用越来越长的前缀，缓存读 Token 不应继续固定在 `6,656`。

## 7. PromptCacheContext v5

领域对象包含：

```java
PromptCacheContext {
    String conversationKey;
    String stablePrefixKey;
    String routingKey;
    String stablePrefixVersion;
}
```

### 7.1 conversationKey

`conversationKey` 是 Key 版本、`ownerKey` 和 `conversationNo` 的 SHA-256 摘要。原始租户、用户和会话标识不会发送给 Provider。

### 7.2 stablePrefixKey

`stablePrefixKey` 对以下内容做规范化摘要：

- Key 算法版本；
- capability `autonomous_agent`；
- Provider 和 Model；
- DB 管理的稳定 System Prompt；
- 按 Tool Code 排序后的 description、guidance、input/output Schema。

Prompt 或 Tool 协议变化会自动生成新的稳定前缀身份。

### 7.3 routingKey 与 16 分片

v4 之前的会话独占 routing key 会降低不同会话之间复用同一静态前缀的概率。v5 改为：

```text
stablePrefixKey + deterministic conversation shard(0..15)
```

最终再做 SHA-256，生成 `cynexus-agent-...`。相同稳定前缀可以在最多 16 个缓存分区中复用，不暴露原始会话标识，也避免所有流量集中在一个 Key。

分片数是吞吐量和跨会话复用率的折中。调整分片规则或稳定前缀序列化时必须升级 Key 版本。

## 8. HTTP 无状态原生回放

### 8.1 第一轮

```json
{
  "model": "gpt-5.4-mini",
  "store": false,
  "prompt_cache_key": "cynexus-agent-...",
  "input": [
    {"role": "system", "content": "稳定系统提示"},
    {"role": "user", "content": "用户问题"},
    {"role": "system", "content": "runtime-1"}
  ],
  "tools": ["按 code 稳定排序的 Tool 定义"]
}
```

Adapter 保存 Response 的完整 `output[]`，不能只提取可见文本。

### 8.2 Tool 续轮

假设上一轮返回：

```json
[
  {"type": "reasoning", "encrypted_content": "opaque..."},
  {"type": "function_call", "id": "fc_1", "call_id": "call_1", "name": "...", "arguments": "{}"}
]
```

Tool 执行后，下一轮 input 顺序必须是：

```json
[
  {"role": "system", "content": "稳定系统提示"},
  {"role": "user", "content": "用户问题"},
  {"role": "system", "content": "runtime-1"},
  {"type": "reasoning", "encrypted_content": "opaque..."},
  {"type": "function_call", "id": "fc_1", "call_id": "call_1", "name": "...", "arguments": "{}"},
  {"type": "function_call_output", "call_id": "call_1", "output": "..."},
  {"role": "system", "content": "runtime-2"}
]
```

`reasoning`、`function_call`、Assistant message、`phase` 和未来 Provider item 都作为 opaque JSON 原样回放。不能把它们重新拼成普通 Assistant 文本。

### 8.3 为什么 runtimeContext 也要保存

`runtimeContext` 是上一轮 output 产生前最后一个 input item，包含当前日期、预算和运行状态。若跨 Run 恢复时丢失它，历史 prefix 会从该位置开始不同。

因此每个 Assistant 持久化消息同时保存“产生该输出前的 runtimeContext”和“该响应的 provider output”。回放顺序固定为 runtimeContext 在前、provider output 在后。

## 9. 数据持久化与边界

`cynexus_appl_ai.ai_agent_message` 新增：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| `runtime_context` | `text` | 产生该 Assistant 输出前的动态系统输入 |
| `provider_output` | `jsonb` | 完整 Responses `output[]` opaque items |

Infra Adapter 负责解析、验证和回放 Provider JSON；Domain 只负责时序、生命周期和持久化，不依赖 Jackson 的 Provider 字段结构。

旧消息没有这两个字段时仍使用可见文本和结构化 Tool Call 的兼容路径。只有存在原生 provider output 的 Tool Call，其关联 Tool Result 才允许跨 Run 回放，避免把 Tool Result 绑定到 Provider 不认识的 Call ID。

## 10. 普通、SSE 和 WebSocket 捕获

三条 Responses 路径都必须产生同一个领域字段 `providerOutputJson`：

- 普通 HTTP：读取根对象 `output`；
- SSE：读取 `response.completed.response.output`；
- WebSocket：读取 `response.completed.response.output`。

增量事件仍用于前端实时文本和 Tool 参数展示，最终 `response.completed` 中的完整 output 才是后续无状态回放的权威数据。

## 11. 推理与 phase 连续性

OpenAI 官方建议推理模型在 Tool 链中回传自最后一次用户消息之后的全部 reasoning、function call 和 function call output。对于 GPT-5.4/GPT-5.5 的长 Tool 流程，还应保留 Assistant `phase`：

- `commentary`：中间状态、Tool 前说明；
- `final_answer`：最终答案。

丢失 `phase` 可能使模型把中间说明误认为最终答案。原样保存完整 output 可以同时解决 reasoning 和 phase 连续性。

## 12. 安全与脱敏

持久化前继续经过 `AgentSensitiveDataPolicy`：

- Tool arguments 中的 `apiKey`、token、email 等字段被脱敏；
- runtimeContext 中的敏感文本被脱敏；
- `encrypted_content` 不按业务敏感字段处理，保持 opaque 值以便回放；
- 同一 Run 内存中的原生 output 不经过数据库往返，保持无损；
- 跨 Run 回放优先遵守产品既有持久化安全策略，因此敏感 Tool 参数可能已被替换为 `[REDACTED]`。

这是一项明确的安全取舍：不能为了缓存率把明文凭据持久化到会话表。

## 13. xhhai 当前绑定

`autonomous_agent` 的 xhhai 绑定应包含：

```json
{
  "wire_api": "responses",
  "responses_transport": "http",
  "disable_response_storage": true,
  "prompt_cache_retention": "24h"
}
```

必须移除 `responses_state_mode`。迁移同时清空 xhhai 会话中旧的 `provider_response_id` 状态，避免继续发送 Provider 已拒绝的 HTTP continuation。

如果 xhhai 后续开放稳定的 WebSocket v2 账号池，可以重新启用 `responses_transport=websocket` 和连接内 `previous_response_id`，但应先通过独立探测和多 Tool 回归测试。

## 14. CACHE_USAGE 遥测

每次模型调用写一条 `CACHE_USAGE` 事件，重点字段：

```json
{
  "turn": 3,
  "inputTokens": 27375,
  "cachedInputTokens": 18432,
  "cacheWriteTokens": 0,
  "cacheHitRatio": 0.6733,
  "cacheKeyHash": "sha256...",
  "stablePrefixHash": "sha256...",
  "requestInputHash": "sha256...",
  "inputItemCount": 12,
  "continuationMode": "STATELESS_REPLAY",
  "responseStorageEnabled": false
}
```

`STATELESS_REPLAY` 表示请求中存在至少一组 Provider 原生 output 回放。第一轮没有历史 output 时仍显示 `STATELESS`。

遥测只保存摘要、计数和模式，不保存原始 Prompt、Tool Result、API Key、Response ID 或用户标识。

## 15. 排障 SQL

### 15.1 查看逐轮缓存增长

```sql
SELECT run_no,
       sequence_no,
       details_json ->> 'turn' AS turn,
       details_json ->> 'continuationMode' AS mode,
       (details_json ->> 'inputTokens')::integer AS input_tokens,
       (details_json ->> 'cachedInputTokens')::integer AS cached_tokens,
       round(100 * (details_json ->> 'cacheHitRatio')::numeric, 2) AS hit_percent,
       details_json ->> 'inputItemCount' AS input_items
FROM cynexus_appl_ai.ai_agent_run_event
WHERE type = 'CACHE_USAGE'
ORDER BY occurred_at DESC
LIMIT 50;
```

### 15.2 检查 xhhai 绑定

```sql
SELECT b.capability,
       p.code AS provider_code,
       b.model_override,
       b.params
FROM cynexus_appl_ai.ai_usage_bindings b
JOIN cynexus_appl_ai.ai_providers p ON p.id = b.provider_id
WHERE b.capability = 'autonomous_agent'
  AND b.deleted_at IS NULL
  AND p.deleted_at IS NULL;
```

### 15.3 检查原生 output 是否落库

```sql
SELECT conversation_no,
       count(*) FILTER (WHERE provider_output IS NOT NULL) AS native_output_messages,
       count(*) FILTER (WHERE runtime_context IS NOT NULL) AS runtime_context_messages,
       max(created_at) AS latest_message_at
FROM cynexus_appl_ai.ai_agent_message
GROUP BY conversation_no
ORDER BY latest_message_at DESC
LIMIT 20;
```

新会话经过至少一次模型响应后，Assistant 消息应出现 `provider_output`；Tool 续轮和下一次跨 Run 请求应出现 `STATELESS_REPLAY`。

## 16. 如何判断优化有效

同一会话的预期趋势：

1. 第一轮为 `STATELESS`，通常未命中或只命中共享静态前缀；
2. 第二个模型调用开始为 `STATELESS_REPLAY`；
3. `provider_output` 和 `runtime_context` 持续落库；
4. 多个 Tool 往返后，`cachedInputTokens` 应突破旧的 `6,656` 平台；
5. `inputTokens` 增长时，缓存读 Token 应在后续请求中逐步增长，而不是每轮固定；
6. 请求不再尝试 xhhai WebSocket，也不再发送 HTTP `previous_response_id`。

命中不会在同一请求内“立即缓存刚生成的 output”。某轮 output 要到下一轮作为 input 重放后才有机会进入新的缓存前缀，因此至少需要多个模型/Tool 往返才能观察增长。

Codex 的高缓存数字通常来自很长、非常稳定的系统指令、工具协议、项目上下文和完整会话历史。应用不能仅靠一个 `prompt_cache_key` 制造同等命中，前缀本身必须足够长且完全一致。

## 17. 长会话与 Compaction

无状态全量回放会使请求体和上下文持续增长。OpenAI 官方提供两种 compaction：

- `context_management` 的服务端 compaction；
- 独立 `/responses/compact` 端点。

两者都会返回 opaque compaction item。无状态客户端必须像其他 output item 一样保存和回放。出现最新 compaction item 后，才可以按官方规则丢弃它之前的历史。

xhhai 当前是否支持 compaction 尚未验证，因此 v4 不默认发送相关字段。未来应以 Provider 能力探测、绑定参数开关和回归测试方式接入，不能直接对所有 OpenAI-compatible Provider 开启。

## 18. 测试清单

- Tool 顺序变化不改变 `stablePrefixKey`；
- 相同稳定前缀的 routing key 最多分布在 16 个稳定分片；
- Prompt、Model 或 Tool Schema 变化会生成新稳定前缀；
- 普通 HTTP、SSE 和 WebSocket 都捕获完整 output；
- replay 顺序为历史 runtime、原生 output、Tool Result、新 runtime；
- reasoning `encrypted_content` 和 Assistant `phase` 被保留；
- 非原生 Tool Call 不伪造 Provider output；
- 持久化脱敏敏感 Tool 字段但保留 opaque reasoning；
- 旧消息没有 provider output 时继续兼容；
- 遥测能区分 `STATELESS` 与 `STATELESS_REPLAY`；
- xhhai 绑定固定为 Responses HTTP、`store=false` 且不使用 state mode。

## 19. 发布、回滚和降级

发布顺序：

1. 执行 `20260726_add_agent_stateless_responses_replay.sql`；
2. 执行 v4 中文知识库迁移并创建新的索引任务；
3. 发布同步版本的 AI/UI Release 镜像；
4. 新建 Agent 会话执行多个 Tool 往返；
5. 检查 `provider_output`、`runtime_context` 和 `CACHE_USAGE`。

紧急降级时可以保留数据库列，只需把绑定切回兼容的 `wire_api` 或移除 Provider 不支持的缓存参数。不要恢复 xhhai HTTP `previous_response_id`，除非 Provider 已明确修复并经过验证。

## 20. 参考资料

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- OpenAI Conversation State：<https://developers.openai.com/api/docs/guides/conversation-state>
- OpenAI Reasoning：<https://developers.openai.com/api/docs/guides/reasoning>
- OpenAI Compaction：<https://developers.openai.com/api/docs/guides/compaction>
- OpenAI Responses WebSocket：<https://developers.openai.com/api/docs/guides/websocket-mode>
