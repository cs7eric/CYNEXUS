# Agent Prompt Cache 企业级架构与实现指南

## 1. 阅读目标

本文用于说明 Cynexus Agent 的 Prompt Cache（提示词缓存）为什么存在、它和 Redis/响应缓存有什么区别、不同模型厂商的实现差异，以及本项目当前代码是如何接入的。阅读完成后，应该能够回答以下问题：

1. Agent 的多轮调用到底缓存了什么？
2. 为什么缓存不能简单地用 `prompt` 字符串做 Redis Key？
3. OpenAI、Grok、Anthropic、Gemini 和本地 vLLM 的缓存机制为什么不能用一套 HTTP 参数统一处理？
4. `PromptCacheContext`、`PromptCacheStrategy` 和 `PromptCacheStrategyRegistry` 各自负责什么？
5. 如何判断缓存真的生效，而不是仅仅把 `prompt_cache_key` 发给了模型？
6. 多租户、敏感数据、工具结果和缓存失效应该如何处理？

本文描述的是模型调用层的第一版企业级接入。它不承诺所有 Provider 都能命中缓存，也不把 Provider 的 Prompt Cache 误当成应用层响应缓存。

## 2. 先区分三种缓存

工程中常说的“AI 缓存”至少包含三类完全不同的能力。

| 类型 | 是否还调用模型 | 缓存内容 | 主要收益 | 主要风险 |
| --- | --- | --- | --- | --- |
| Provider Prompt Cache | 是 | 模型输入前缀对应的 KV/前缀状态 | 降低输入 Token 成本和 Prefill 延迟 | Provider 规则不同，前缀变化会失效 |
| 应用响应缓存 | 否 | 一次完整请求的最终答案 | 直接减少模型调用 | 权限、时效、个性化和数据污染风险 |
| 工具/数据缓存 | 不一定 | 工具查询或外部数据结果 | 减少重复访问行情、知识库、HTTP 服务 | 数据新鲜度和租户隔离风险 |

本项目当前实现的是第一类。Agent 每一轮仍然调用模型，模型仍然会根据最新用户输入、历史消息和工具结果重新推理；Provider 只尝试复用已经处理过的稳定输入前缀。因此它不会导致旧答案被错误复用，也不会跳过工具执行。

### 2.1 为什么 Prompt Cache 能节省 Token

一次模型调用的输入通常可以拆成：

```text
稳定系统规则
  + 工具定义、参数 Schema、调用指导
  + 已经确定的长期上下文
  + 当前会话历史
  + 本轮用户问题和工具结果
```

多轮 Agent 中，系统规则和工具定义通常很长，而且在同一次运行中重复发送。Provider 可以将已经处理过的前缀保存为内部 KV Cache，后续请求只重新处理变化部分。

需要注意：`input_tokens` 通常仍然表示完整输入 Token 数；Provider 另外通过 `cached_tokens`、`cache_read_input_tokens` 等字段报告其中有多少是命中缓存的 Token。缓存命中 Token 不等于请求没有发送这些文本，也不等于模型没有看到这些内容。

## 3. 企业级设计原则

### 3.1 缓存不是正确性的来源

模型输出、工具结果和数据库状态仍然以实时请求为准。Prompt Cache 只优化模型对重复前缀的计算，不改变业务语义。任何依赖权限、行情、库存、余额或外部接口实时状态的内容，都不能因为命中 Prompt Cache 而跳过工具查询。

### 3.2 Provider 差异必须留在基础设施层

不同 Provider 的缓存协议不一致：有的使用请求体字段，有的使用 HTTP Header，有的需要显式声明缓存断点，有的通过缓存资源 ID 管理。Domain 层只表达“这次 Agent 请求具有怎样的缓存上下文”，不能依赖 OpenAI 或 Grok 的字段名。

### 3.3 Key 必须可解释、可失效、不可泄露

缓存 Key 要能在以下变化发生时自动失效：模型变化、Provider 变化、工具描述变化、工具 Schema 变化、缓存算法版本变化。同时不能把租户号、用户号、会话号和原始 Prompt 直接发送给 Provider 或写入日志。

### 3.4 观测命中率，而不是只观测请求成功率

Provider 接受 `prompt_cache_key` 只能证明请求格式被接受，不能证明发生了缓存命中。必须结合响应中的缓存 Token 字段、输入 Token、延迟和成本数据进行验证。

## 4. 当前项目的整体调用链

```text
AgentRunExecutionDomainService
  1. 解析 Provider、Model 和已启用 Tool
 2. AgentPromptCacheKeyPolicy 按当前轮次的 Tool 集合生成 PromptCacheContext
 3. 每一轮构造 AgentModelTurnRequest，并携带当前轮次的 Context
  4. AiCompletionGateway 转发到 OpenAiCompatibleChatAdapter
  5. PromptCacheStrategyRegistry 根据模型 Provider 选择策略
  6. 策略修改请求 Header/Body，并解析 Provider usage
  7. AgentRunExecutionDomainService 累加普通 Token 和缓存 Token
  8. AgentRunPO 持久化到 ai_agent_run
```

### 4.1 代码职责分布

| 文件/模块 | 职责 |
| --- | --- |
| `cynexus-ai-common/.../AiConstants.java` | 缓存能力名、Key 版本、Provider 字段名等跨 AI 模块常量 |
| `cynexus-ai-domain/.../PromptCacheContext.java` | Provider 无关的缓存上下文 Value Object |
| `cynexus-ai-domain/.../AgentPromptCacheKeyPolicy.java` | 生成稳定 Key，处理排序、摘要和隐私隔离 |
| `AgentRunExecutionDomainService.java` | 每轮按实际 Tool 集合生成 Context，并汇总 usage |
| `cynexus-ai-infra/.../PromptCacheStrategy.java` | Provider 缓存策略端口/协议 |
| `OpenAiPromptCacheStrategy.java` | OpenAI 请求字段和 usage 解析 |
| `XaiPromptCacheStrategy.java` | Grok/xAI Header、Responses 字段和 usage 解析 |
| `PromptCacheStrategyRegistry.java` | 策略选择；对 OpenAI-compatible Adapter 默认启用通用缓存提示 |
| `GenericPromptCacheStrategy.java` | 所有 OpenAI-compatible Provider 的通用 `prompt_cache_key` 和 usage 解析 |
| `OpenAiCompatibleChatAdapter.java` | HTTP 请求装配、流式/非流式传输和策略调用 |
| `ai_agent_run` | 持久化 `cached_input_tokens` 和 `cache_write_tokens` |

这符合 DDD 分层：Domain 只创建缓存语义和 Key，Infra 才知道 HTTP 字段和 Provider usage JSON；不把 OpenAI 字段泄漏到 Domain。

## 5. PromptCacheContext 详解

当前上下文包含三个字段：

```java
PromptCacheContext {
    String conversationKey;
    String stablePrefixKey;
    String stablePrefixVersion;
}
```

### 5.1 `conversationKey`

它用于表达“这是哪个隔离的会话”。当前实现对以下字符串计算 SHA-256：

```text
v1|conversation|ownerKey|conversationNo
```

然后加上 `cynexus-conversation-` 前缀。原始 `ownerKey` 和 `conversationNo` 不会发送给 Provider。不同租户、不同用户或不同会话会得到不同的 Key，避免 Provider 的会话路由或缓存范围意外混用。

### 5.2 `stablePrefixKey`

它用于表达“这组模型输入的稳定定义是什么”。当前实现将以下内容纳入摘要：

```text
v1
autonomous_agent
providerIdentity
modelCode
每个 Tool 的 code
每个 Tool 的 description
每个 Tool 的 guidanceJson
每个 Tool 的 inputSchemaJson
每个 Tool 的 outputSchemaJson
```

Tool 会先按 `code` 排序，再计算 SHA-256。因此同一批 Tool 只是顺序不同，不会导致 Key 变化；Tool 的描述、指导语或 Schema 改变则会导致 Key 变化，避免旧的工具协议继续复用。

### 5.3 特意没有放进 Key 的内容

以下内容不能放进稳定前缀 Key：

- 当前用户问题；
- 当前日期和时间；
- 本轮已使用工具数量；
- 工具返回结果；
- 时间戳、请求 ID、Trace ID；
- 原始 Prompt 和用户隐私数据。

这些字段会频繁变化，放入 Key 会造成缓存碎片化；放入 Provider 请求前缀又会减少可复用长度。它们应该作为动态请求内容处理。

### 5.4 Key 版本

`stablePrefixVersion` 当前为 `v3`。Tool 定义始终参与稳定前缀 Key，即使最终回答轮禁止继续调用工具，也继续发送相同的 Tool 定义，只通过 `tool_choice=none` 关闭调用。这样最终回答不会因为请求结构突然从“有 Tool”变成“无 Tool”而丢失前缀缓存。当 Key 算法、Tool 序列化格式或隔离规则发生不兼容变化时，也必须升级版本，让 Provider 的旧缓存自然淘汰。

## 6. Provider 实现差异

### 6.1 OpenAI

OpenAI 主要使用自动 Prefix Caching。满足 Provider 的最小 Token 条件并且请求前缀完全一致时，模型可能复用之前的计算结果。请求可以携带 `prompt_cache_key`，它用于将相似请求路由到更合适的缓存分区；它不是把任意两个不同 Prompt 强行视为相同 Prompt 的开关。

当前项目行为：

```json
{
  "model": "...",
  "prompt_cache_key": "cynexus-prefix-...",
  "messages": [],
  "tools": []
}
```

响应中的缓存 usage 兼容以下结构：

```json
{
  "usage": {
    "prompt_tokens": 3200,
    "prompt_tokens_details": {
      "cached_tokens": 2048
    }
  }
}
```

有些兼容接口使用 `input_tokens_details`，流式封装还可能把缓存数据放在 usage 的平面字段中。当前解析器会同时扫描 `prompt_tokens_details`、`input_tokens_details` 和平面字段，并在多个别名同时出现时取较大的有效值，避免因为某个空的 details 对象遮蔽真实的 cache read/write。当前 Agent 对所有经过 `OpenAiCompatibleChatAdapter` 的 Provider 和 Model 默认发送通用 `prompt_cache_key`，并尝试解析通用缓存 usage 字段；xAI/Grok 额外发送自己的会话 Header。Provider 是否实际命中，仍取决于上游是否透传并实现该协议。

### 6.2 Grok/xAI

xAI 的 Chat Completions 和 Responses 接口使用方式不同：

- Chat Completions 使用 `x-grok-conv-id` Header 做会话路由；
- Responses 请求体使用 `prompt_cache_key`；
- usage 通常从 `prompt_tokens_details.cached_tokens` 或 `input_tokens_details.cached_tokens` 读取。

当前项目的 xAI 策略会把经过摘要的 `conversationKey` 放入 `x-grok-conv-id`，把 `stablePrefixKey` 放入 Responses 的 `prompt_cache_key`。这样既保持同一个会话的路由稳定，又不把会话原始标识暴露给 xAI。

### 6.3 Anthropic

Anthropic 的缓存模型通常使用显式 `cache_control` 断点，或者由 Provider 的自动缓存能力决定缓存范围；同时存在 cache creation 和 cache read 的不同计费/usage 字段。它不是在 OpenAI Chat Completions 请求里加一个 `prompt_cache_key` 就能实现。

因此 Anthropic 应该由独立的 Anthropic Adapter/Strategy 实现：

```text
Anthropic 请求
  -> content block
  -> cache_control: { type: "ephemeral" }
  -> cache_creation_input_tokens / cache_read_input_tokens
```

当前第一版没有向 Anthropic 请求发送 OpenAI 字段，避免协议误用。

### 6.4 Gemini

Gemini 同时存在隐式缓存和显式 Cached Content 资源两类能力。显式缓存通常需要先创建缓存资源，再在请求中引用资源名；缓存资源具有 TTL、模型和内容约束。它和 OpenAI 的前缀 Key 不是同一种生命周期模型。

后续接入 Gemini 时，建议将缓存资源生命周期放在 Infra，并把 `cachedContent` 资源 ID 和 TTL 作为 Provider-specific Context；不要把它们塞进通用的 `PromptCacheContext`。

### 6.5 vLLM 和自托管模型

vLLM 的 Automatic Prefix Caching 发生在服务端 KV Block 层。它通常根据 Token Block、LoRA/Adapter、租户隔离 Salt 等信息计算缓存身份。应用层不应该用 Redis 仿造 KV Cache，也不应该把完整 Prompt 复制到 Redis；应用只需要保证稳定前缀和安全的请求路由。

## 7. Strategy + Registry 设计

### 7.1 为什么不用 Adapter 中的大量 if/else

如果在 `OpenAiCompatibleChatAdapter` 中直接编写：

```java
if (provider.equals("openai")) { ... }
else if (provider.equals("grok")) { ... }
else if (provider.equals("anthropic")) { ... }
```

每增加一个 Provider，HTTP 请求、流式请求、usage 解析都会继续膨胀，测试也会互相影响。更严重的是，Provider 字段容易被错误地发送给不支持它的兼容接口。

当前采用策略接口：

```java
interface PromptCacheStrategy {
    boolean supports(AiModelBO model);
    Map<String, String> headers(AiModelBO model, PromptCacheContext context);
    void enrichPayload(AiModelBO model, PromptCacheContext context,
                       Map<String, Object> payload, boolean responsesApi);
    PromptCacheUsage extractUsage(JsonNode usage);
}
```

Registry 按顺序选择第一个支持该模型的策略。xAI/Grok 先使用专用策略，OpenAI 使用 OpenAI 策略，其他 Provider 使用 `GenericPromptCacheStrategy`。因此不会因为 Provider 名称未知而在应用侧主动关闭缓存提示；但这不代表上游一定会命中。

### 7.2 目前的策略选择

```text
Grok/xAI -> XaiPromptCacheStrategy
OpenAI   -> OpenAiPromptCacheStrategy
其他     -> GenericPromptCacheStrategy
```

专用策略通过 `providerIdentity` 和 `providerCode` 识别 Provider；无法识别的 Provider 会落到通用策略，以覆盖 `xhhai`、`custom`、`deepseek`、`dashscope`、`minimax` 等 OpenAI-compatible 路由。

## 8. 当前 Agent 请求中的稳定性边界

Provider Prompt Cache 按输入前缀复用已经处理过的模型状态。当前实现采用以下顺序：

```text
稳定 System Prompt
  + Conversation History（用户、助手 Tool Call、Tool Result）
  + 运行时上下文（日期、预算、已用工具数、强制收敛标记）
```

运行时上下文被放在每轮历史之后，避免日期、预算和工具计数污染稳定 System Prompt。每轮模型返回后，当前轮已经发送的运行时上下文会加入本次运行的内存历史，下一轮先复用它，再追加助手 Tool Call、Tool Result 和新的运行时上下文。因此连续 Tool 调用时，上一轮完整请求可以成为下一轮的缓存前缀，而不是只能命中固定的 System Prompt 和 Tool 定义。运行时上下文仍然保留为最后一条 `system` 消息，日期和预算约束不会被删除。跨 Run 的持久化历史不保存这些短生命周期上下文，只共享稳定 System Prompt 和 Tool 定义缓存。

Tool 定义是请求级结构，而不是会话历史。无论当前是否允许继续调用工具，Adapter 都按 code 排序发送相同的 Tool 定义，并用 `tool_choice=auto/none` 控制调用能力，保证 Key 与实际请求序列稳定一致。

这不是一个可以“强制命中”的本地缓存。最终命中仍由 Provider 的模型门槛、最小缓存粒度、TTL、容量和实际协议支持决定，必须结合响应中的 `cached_input_tokens` 观察效果。

## 9. Usage、成本和数据库持久化

### 9.1 三类 Token

| 字段 | 含义 |
| --- | --- |
| `input_tokens` | Provider 报告的完整输入 Token，缺失时由适配器估算 |
| `cached_input_tokens` | Provider 报告的缓存读取 Token |
| `cache_write_tokens` | Provider 报告的缓存创建/写入 Token |
| `output_tokens` | 输出 Token |
| `total_tokens` | Provider 报告的总 Token或估算值 |

`cached_input_tokens` 不应该从 `input_tokens` 中扣除后再覆盖原字段，否则会丢失真实请求规模。成本计算要根据具体 Provider 的缓存折扣规则单独扩展，不能假设所有 Provider 的缓存读写价格相同。

### 9.2 持久化路径

```text
Provider JSON usage
  -> PromptCacheStrategy.extractUsage
  -> AgentModelTurnResponse
  -> AgentRunExecutionDomainService 累加
  -> AgentRunBO
  -> AgentRunPO
  -> cynexus_appl_ai.ai_agent_run
  -> AgentRunDTO / Agent 页面
```

本版本增加：

```sql
cached_input_tokens integer not null default 0
cache_write_tokens integer not null default 0
```

Provider 没有返回缓存字段时按 `0` 处理，不会让 Agent 运行失败。

### 9.3 如何验证真的命中

单看请求日志中的 `prompt_cache_key` 不够。建议记录以下非敏感指标：

```text
provider
model
cache_key_version
input_tokens
cached_input_tokens
cache_write_tokens
cached_ratio = cached_input_tokens / input_tokens
request_latency_ms
usage_source = REPORTED / ESTIMATED
```

不要记录原始 Prompt、完整 Tool Result、API Key、ownerKey 或未经脱敏的 Cache Key。可以记录 Key 的短摘要或版本，不要让日志成为跨租户关联工具。

## 10. 安全与多租户隔离

### 10.1 Provider Prompt Cache 的隔离

不同 Provider 对缓存隔离的承诺不同。应用层必须做到：

- 任何会话路由 Key 都使用不可逆摘要；
- xAI 会话 Header 不使用原始会话号；
- 不把用户问题、工具结果和敏感业务数据写入共享 Redis Prompt Cache；
- 不因为两个用户使用相同 Tool 定义，就在应用层共享他们的会话历史；
- Provider/model 变化时重新计算稳定前缀 Key；
- Key 算法升级时升级版本。

### 10.2 Redis 应该缓存什么

Redis 适合缓存：

- Agent 幂等请求状态；
- 限流计数；
- Runtime Lease 和取消状态；
- 明确有 TTL 和 freshness 策略的只读工具结果；
- 明确绑定租户、用户、工具、参数和权限范围的应用数据。

Redis 不应该直接承担模型 KV Prompt Cache，也不应该用完整 Prompt 做无边界 Key。响应缓存必须额外检查权限、数据版本、时效、参数归一化和敏感字段脱敏。

### 10.3 工具结果缓存 Key 示例

一个只读工具结果缓存至少需要考虑：

```text
tenantKey
principalKey
toolCode
normalizedArguments
dataDate
freshnessPolicyVersion
```

如果工具结果包含用户私有数据，不能只使用 `toolCode + arguments`，否则会产生跨用户数据泄漏。

## 11. 测试策略

### 11.1 Key Policy 单元测试

必须覆盖：

1. 相同 owner、conversation、model、Tool 生成相同 Context；
2. Tool 顺序变化不改变 `stablePrefixKey`；
3. conversation 变化改变 `conversationKey`；
4. Tool description、guidance 或 Schema 变化改变 `stablePrefixKey`；
5. 原始 owner 和 conversation 不出现在 Key 中；
6. Key version 变化可以产生新 Key。

### 11.2 Adapter Contract 测试

OpenAI：

- Body 包含 `prompt_cache_key`；
- 可以解析 `prompt_tokens_details.cached_tokens`；
- 可以解析 cache write 字段；
- 流式和非流式结果都能持久化缓存 usage。

Grok：

- Chat 请求包含 `x-grok-conv-id`；
- Responses 请求包含 `prompt_cache_key`；
- 不同会话使用不同 Header；
- usage 别名可以正常解析。

Unsupported Provider：

- 不增加 Provider 专用缓存 Header；
- 仍增加通用 `prompt_cache_key`，由上游决定是否接受和命中；
- 原有请求结构不改变。

### 11.3 生产验证

灰度发布后，按 Provider/Model 比较以下数据：

```text
缓存启用前后的平均 input latency
缓存命中率
cached_input_tokens 占比
cache_write_tokens
每次 Agent run 的输入成本
Provider 4xx/5xx 和超时率
```

如果 Provider 返回错误或缓存字段格式变化，应该通过策略降级到 no-op，而不是让整个 Agent 运行失败。必要时可以在模型 `extra` 或 Provider 配置中增加 feature flag。

## 12. 发布和迁移流程

本功能涉及 Java、数据库和知识库文档，发布顺序建议为：

1. 先执行 `20260726_add_ai_agent_prompt_cache_usage.sql`，增加 Agent run 的缓存 usage 字段；
2. 执行 `20260726_seed_agent_prompt_cache_architecture_kb.sql`，写入文档、chunk 和索引任务；
3. 发布 AI 服务，使新代码能够读取新字段并发送 Provider 缓存参数；
4. 观察 Provider usage 和数据库字段；
5. 再开启成本报表或更激进的工具结果缓存。

两条迁移都可以重复执行：AI 字段使用 `IF NOT EXISTS`，知识库文档使用固定负数 ID 并重建本文件对应的 chunk/index task。固定负数 ID 是为了和 Snowflake 正数 ID 分开，避免种子数据与正常业务数据冲突。

## 13. 常见误区与问答

### Q1：设置了 `prompt_cache_key`，是不是就一定命中？

不是。它通常是路由/分区提示，仍然要满足模型、Provider、前缀顺序、Token 最小长度、TTL 和缓存容量等条件。最终要看响应 usage 的 cached token 字段。

### Q2：为什么不把完整 Prompt 做成 Redis Key，然后直接返回旧答案？

那是响应缓存，不是 Prompt Cache。它会跳过模型调用，也可能跳过工具调用；如果权限、行情、知识库内容或工具参数处理不严格，就会返回过期或他人的答案。

### Q3：所有 Provider 都发送缓存提示后，是不是一定会命中？

不是。兼容 Chat Completions 只代表基础请求格式相似，不代表上游支持 `prompt_cache_key` 或相同 usage 结构。当前通用策略会发送 `prompt_cache_key`，但只有上游接受、透传并在响应中返回 `cached_tokens` 等字段，才算真正命中；若某个网关拒绝该字段，需要在网关侧开启兼容转发或增加 Provider 专用策略。

### Q4：缓存 Token 是否应该从总输入 Token 中减掉？

不应该。`input_tokens` 表示请求输入规模，`cached_input_tokens` 是其中的子集。成本估算要基于 Provider 的缓存读取价格另行计算。

### Q5：工具结果变化会不会让旧数据被模型缓存？

Provider 缓存的是输入前缀的模型计算状态，不会把新的工具结果自动变成旧工具结果。每次请求仍然要发送最新工具消息。真正缓存工具结果是另一项需要 freshness 和权限控制的功能。

### Q6：为什么本项目没有把 Anthropic 和 Gemini 也塞进同一个 OpenAI Adapter？

因为它们的缓存生命周期和协议不同。统一的是 Domain 的缓存语义，不能强行统一 Provider 的 HTTP 协议。后续应添加独立 Strategy/Adapter，并复用安全、观测和 usage 规范。

## 14. 术语表

| 术语 | 解释 |
| --- | --- |
| Prefix Cache | 对输入前缀的模型中间计算状态进行复用 |
| KV Cache | Transformer 推理过程中保存的 Key/Value 中间状态 |
| Prefill | 模型处理输入 Prompt 的阶段 |
| Cache Read | 读取已经存在的 Provider 缓存 |
| Cache Write | 为新的稳定前缀创建 Provider 缓存 |
| Stable Prefix | 多次请求中保持不变且位于输入前部的内容 |
| No-op Strategy | 不修改请求、也不解析 Provider 专属字段的降级策略 |
| Response Cache | 直接缓存完整模型答案、可以跳过模型调用的应用层缓存 |
| Freshness | 工具或外部数据允许被复用的时间/版本边界 |

## 15. 参考资料

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- xAI Prompt Caching：<https://docs.x.ai/developers/advanced-api-usage/prompt-caching>
- Anthropic Prompt Caching：<https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching>
- Gemini Context Caching：<https://ai.google.dev/gemini-api/docs/caching>
- vLLM Automatic Prefix Caching：<https://docs.vllm.ai/en/latest/features/automatic_prefix_caching/>
- LiteLLM Prompt Caching：<https://docs.litellm.ai/docs/completion/prompt_caching>
