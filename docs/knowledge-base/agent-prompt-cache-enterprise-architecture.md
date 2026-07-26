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

## 6. 当前总体方案

```text
稳定 System Prompt + 排序后的 Tool Schema
                    |
                    v
             stablePrefixKey
                    |
       conversation hash -> 会话独占路由身份
                    |
                    v
        prompt_cache_key (v6 routing key)

Responses HTTP, store=false
  用户输入 + runtime-1
       -> output-1[]
  原样回放 output-1[] + tool-result-1 + runtime-2
       -> output-2[]
  原样回放 output-2[] + tool-result-2 + runtime-3
       -> output-3[]
```

每轮请求都携带完整无状态 input，但前缀从稳定 Prompt、Tool、历史 input/output 开始保持一致。Provider 可以复用越来越长的前缀，缓存读 Token 不应继续固定在 `6,656`。

## 7. PromptCacheContext v6

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

### 7.3 routingKey 与会话隔离

v5 曾按稳定前缀把所有会话压缩到 16 个共享 Key，希望提高不同会话对静态 Prompt 与 Tool Schema 的复用率。真实 xhhai 链路证明，共享 Key 会让不同会话在首个动态输入处分叉，缓存只能停在共同的静态前缀。v6 改为：

```text
stablePrefixKey + SHA-256(ownerKey + conversationNo) + deterministic shard marker
```

最终再做 SHA-256，生成 `cynexus-agent-...`。每个会话获得独立且稳定的 Key，Provider 可以沿该会话的完整 input/output/tool-result 前缀持续扩展缓存。原始租户、用户和会话标识不会外发。

`shard marker` 只保留用于可观测性和未来路由兼容，不再决定 Key 的唯一性。调整会话摘要、分片规则或稳定前缀序列化时必须升级 Key 版本。

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
WHERE event_type = 'CACHE_USAGE'
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
- 相同会话和稳定前缀生成相同 routing key，不同会话生成不同 routing key；
- Prompt、Model 或 Tool Schema 变化会生成新稳定前缀；
- 普通 HTTP、SSE 和 WebSocket 都捕获完整 output；
- 当前 Run 的 replay 顺序为历史 runtime、原生 output、Tool Result、新 runtime；
- 跨 Run 只投影完成状态的可见 user/assistant，不携带旧 Run 的原生 wire item；
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

## 20. xhhai 加密 reasoning 返回差异与 v5 修正

`2026-07-26` 部署 v4 后的真实 Agent Run `AGR-47d95ff97b41` 提供了关键证据：

```text
turn 1: input=12269, cached=0,    mode=STATELESS
turn 2: input=13426, cached=6656, mode=STATELESS_REPLAY
turn 3: input=13744, cached=6656, mode=STATELESS_REPLAY
```

数据库已经保存并回放 `provider_output`，routing key 和 stable prefix 也保持一致，但 reasoning item 只有 `type` 与 `summary`，没有 `encrypted_content`。这说明问题不再是 Cynexus 丢弃完整 output，而是 xhhai 在 `store=false` 时没有按照当前 OpenAI 默认行为自动返回加密 reasoning 状态。

OpenAI Reasoning 指南说明：无状态请求的 reasoning item 应携带 opaque `encrypted_content`，下一次请求必须把它连同完整 output 一起回放。官方当前服务会默认返回该字段，同时仍兼容旧的显式请求形式：

```json
{
  "store": false,
  "include": ["reasoning.encrypted_content"]
}
```

早期直接探测使用的是非流式请求，只能证明 xhhai 非流式 Responses 在显式设置 `include` 后会返回 `encrypted_content`。它没有覆盖 UI Agent 实际使用的 HTTP SSE 链路，因此不能作为真实 Agent 验证结论。

这项修正影响的是 Provider 原生 reasoning 状态连续性，不是把 reasoning 明文写入数据库：`encrypted_content` 对 Cynexus 始终是不解析、不修改的 opaque 字符串。它也是当前固定 `6,656` 平台最直接的根因修复。部署后必须新建会话重新预热；旧消息中没有 `encrypted_content`，无法在客户端补造。

## 21. v6：HTTP SSE 与非流式 Responses 的实测差异

对同一 xhhai 账号、同一 `gpt-5.4-mini`、同一 low reasoning 请求做严格单变量 A/B：

```text
stream=false: HTTP 200, output=reasoning,message, encrypted_content length=996
stream=true:  HTTP 200, response.completed.output=message, encrypted_content length=0
```

两个请求都发送 `store=false` 与 `include=[reasoning.encrypted_content]`，并报告相同的 `3,840` cached input tokens。差异只来自 `stream`。这证明 xhhai 当前 HTTP SSE 兼容层会从最终 output 中移除 encrypted reasoning；Cynexus SSE parser 没有丢字段，因为数据库保存的就是上游 `response.completed.response.output[]` 原值。

真实 Run `AGR-f5293c6e9896` 的两个 assistant output 都只有 reasoning summary，没有 encrypted state。另一个两轮 Run 显示 `13,312 = 2 x 6,656`，说明每次模型调用仍只命中静态 Prompt 与 Tool Schema，不是第二轮命中了 13K。

v6 为 Agent binding 增加 `responses_http_stream_mode`：

- `sse`：适用于能在 `response.completed` 中完整返回 encrypted reasoning 的 Provider；
- `buffered`：发送非流式 Responses，先获取完整 output，再通过现有 UI 事件一次性发出 reasoning summary 和答案文本；
- xhhai 默认使用 `buffered`，同时保持 `responses_transport=http`、`store=false` 与完整无状态回放。

这个模式不会伪造 reasoning，也不会发起第二个补偿请求。它只把同一次 Responses 调用从 SSE 改为 buffered JSON，因此避免双倍 Token 消耗，并绕过本次日志中反复出现的 xhhai Cloudflare SSE `502` 与 TLS EOF 路径。代价是 UI 不再逐 Token 显示，而是在上游完成后一次显示本轮推理摘要和答案。

部署验证必须使用真实 Agent 会话，并同时满足：新 assistant `provider_output` 含非空 `encrypted_content`；后续 Tool 续轮的单次 `cachedInputTokens` 超过旧的 `6,656` 平台；不能再用不带 Tool Schema、runtime context 和 SSE 行为的最小 Demo 代替真实链路验证。

## 22. v7：会话隔离假设及其证伪

`1.1.295-260727-RELEASE` 对真实 Agent 请求增加了不含原文的边界遥测：每个 input item 的 SHA-256、累积前缀 SHA-256、字节数和 item kind，以及 Tool Schema 与请求选项哈希。真实 Run `AGR-d2627dcf79ab` 连续执行六个模型轮次，输入从 `12,294` 增长到 `19,556`，但每个后续请求都只缓存 `6,656` Token。

逐项对比确认：第 2 轮的 stable system、user、旧 runtime context 与第 1 轮完全一致；第 3 至第 6 轮也完整继承此前所有 reasoning、message、function_call、function_call_output 与 runtime context；Tool Schema 和请求选项始终一致。由此排除了历史丢失、JSON 排序、动态日期、reasoning effort 和 encrypted state 变化。

当时的假设是 v5 routing key 只包含 `stablePrefixKey + shard(0..15)`，多个会话共用 Key 后从首个用户输入处分叉，导致 xhhai 只能复用共同静态前缀。v6 因此改成会话独占 Key。这个改动保留，因为它能降低不同会话争用同一显式 Key 的风险，但后续 Release 实测证明它不是 `6,656` 平台的最终根因。

严格 A/B 仍确认了显式 Key 的必要性：使用独占 Key 时三轮 cached input 为 `0 -> 7,552 -> 8,064`；完全不发送 Key 时三轮均为 `0`。但部署 `1.1.296-260727-RELEASE` 后，真实 Run `AGR-84dd6283612c` 在会话独占 Key 下，第 2 至第 7 轮仍全部是 `6,656`。因此“共享 Key 是最终根因”的判断被真实链路证伪。

## 23. v8：动态 system runtime context 才是 6,656 平台的根因

进一步使用真实 22 个 Tool、完整 guidance、相同模型、相同 Responses 参数和相同会话回放结构做单变量 A/B，只改变 runtime context 的消息角色：

```text
runtime role=system: turn 1 = 0/11,190; turn 2 = 6,656/11,237
runtime role=user:   turn 1 = 0/11,178; turn 2 = 10,752/11,203
```

这次 A/B 精确复现了真实 Agent 的 `6,656`。原因是每轮都会追加一条变化的 `system` runtime context。虽然请求 JSON 数组的旧 item 前缀逐项一致，但 Provider 会把 system/developer 指令纳入特殊指令前缀处理；新增的动态 system 消息使内部有效前缀在 Tool Schema/历史之前分叉。仅看请求数组 SHA-256 无法观察这种 Provider 内部规范化。

v8 将服务端生成的 runtime context 作为尾部 `user` 消息发送。稳定 System Prompt 已明确声明该消息是每轮权威运行时值，因此工具预算、当前日期和强制收敛语义不变。历史 runtime context 也按原顺序以 `user` 消息回放；Chat Completions 路径同步补齐历史 runtime context，避免只发送当前动态上下文导致前缀截断。缓存 Key 升级到 v7，避免继续使用旧角色布局预热出的缓存分支。

验收必须同时检查：同一会话 Key 不变；不同会话 Key 不同；runtime context item kind 为 `user`；第 2 轮单次 cached input 明显超过 `6,656`；后续轮次随历史增长；跨 Run 继续同一 conversation 时仍使用同一 Key；UI retry marker 和上游 502 重试保持可见。

## 24. v9：跨 Run 原生回放导致 502

v8 修复后，真实 Run `AGR-c8da91dc6de8` 的单次缓存从第 2 轮开始增长：

```text
turn 1: input=12,214 cached=0
turn 2: input=12,564 cached=12,288
turn 3: input=13,052 cached=12,800
turn 4: input=14,078 cached=12,800
turn 5: input=14,803 cached=13,824
turn 6: input=15,455 cached=14,336
```

这证明 trailing user runtime context 已解决同一 Run 内固定 `6,656` 的问题。但同一 conversation 发起新的 Run 时，上游连续返回 HTTP 502，三个请求重试均在 `CACHE_USAGE` 前失败。失败 Run 还留下了只有 user、没有最终 assistant 的消息尾部。

根因是“持久化会话历史”与“Provider 原生协议历史”混为一体。一个 Run 内的 reasoning、function call、function call output、runtime context 和 encrypted state 共同组成有效 Responses 链，必须按原顺序回放；进入新 Run 后，客户端没有可用的原始 response linkage，却仍把旧 Run 的这些 wire item 与完整 22 Tool Schema 一起提交。xhhai 对这个大型跨 Run 组合返回 502。失败 Run 的孤立 user 再次进入后续请求，会继续污染会话结构。

v9 建立两级历史：

- 当前 Run：保留完整原生 output、Tool Result 和每轮 runtime context，以获得高缓存命中并维持 reasoning 连续性；
- 已完成旧 Run：只保留可见 user 输入与最终 assistant 答案，重新构造普通消息，不携带 `provider_output`、runtime context、Tool calls 或 Tool outputs；
- 只有状态为 `SUCCEEDED` 或 `NEEDS_CLARIFICATION` 且存在非空最终 assistant 的 Run 可以进入跨 Run 历史；
- `FAILED`、`CANCELLED` 和没有最终 assistant 的不完整 Run 整组丢弃，而不是只删除最后一条 Tool 消息。

由于跨 Run input 布局发生变化，缓存 Key 合约同步升级到 v8，避免继续路由到旧版原生跨 Run 回放形成的缓存分支。

这不是删除审计数据。数据库仍保存每个 Run 的完整消息和执行步骤，变化只发生在下一次 Provider 请求的投影层。UI 查看历史、运行审计和故障分析不受影响。

跨 Run 第一次请求的可复用前缀通常短于同一 Run 内下一 Tool 轮，因为旧 Run 的原生 wire transcript 被安全折叠。它仍复用稳定 System Prompt、Tool Schema、会话专属 `prompt_cache_key` 以及能保持一致的可见历史；新 Run 产生 Tool 往返后，缓存会再次沿当前 Run 的完整原生链增长。这里优先保证协议有效和请求可用，不能为追求命中率重放没有 linkage 的孤立 function items。

排障时可先检查每个 Run 是否形成可见闭合对：

```sql
SELECT m.run_no,
       r.status,
       count(*) FILTER (WHERE m.role = 'user') AS user_messages,
       count(*) FILTER (WHERE m.role = 'assistant' AND nullif(trim(m.content), '') IS NOT NULL) AS visible_answers,
       count(*) FILTER (WHERE m.role = 'tool') AS tool_messages
FROM cynexus_appl_ai.ai_agent_message m
JOIN cynexus_appl_ai.ai_agent_run r ON r.run_no = m.run_no
WHERE m.conversation_no = :conversation_no
  AND m.deleted_at IS NULL
  AND r.deleted_at IS NULL
GROUP BY m.run_no, r.status
ORDER BY min(m.sequence_no);
```

验收标准：完成旧 Run 后，同 conversation 的新 Run 能成功返回；失败 Run 的 user 尾部不进入 Provider 输入；跨 Run 输入不含旧 Run 的 reasoning/function_call/function_call_output/runtime context；当前 Run 的 Tool 续轮仍保留原生回放且缓存继续增长；真实上游故障仍显示 retry marker。

## 25. 参考资料

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- OpenAI Conversation State：<https://developers.openai.com/api/docs/guides/conversation-state>
- OpenAI Reasoning：<https://developers.openai.com/api/docs/guides/reasoning>
- OpenAI Compaction：<https://developers.openai.com/api/docs/guides/compaction>
- OpenAI Responses WebSocket：<https://developers.openai.com/api/docs/guides/websocket-mode>
