DO $$
DECLARE
    v_document_id bigint := -202607260001;
    document_content text := $doc$# Agent Prompt Cache 企业级架构与实现指南

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

旧实现存在三个限制：所有会话共享 `stablePrefixKey`；Responses 默认 `store=false` 且没有 `previous_response_id`；本地只重建可见文本和 Tool 消息，没有重放完整 Responses output 或内部状态。因此固定 `6.7K` 更像“静态前缀命中、增长历史未命中”，而不是 Provider 设置了 6.7K 上限。

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

缓存路由按会话隔离，Provider 状态只有在 Provider、Model 和稳定前缀签名完全一致时才复用。

## 5. PromptCacheContext v4

领域对象包含 `conversationKey`、`stablePrefixKey`、`routingKey` 和 `stablePrefixVersion`。

`conversationKey` 是 `ownerKey + conversationNo` 的 SHA-256 摘要，原始租户、用户和会话标识不会发送给 Provider。

`stablePrefixKey` 对 Cache Key 版本、`autonomous_agent`、Provider、Model、DB 管理的稳定 System Prompt，以及按 Tool Code 排序后的 description、guidance、input/output Schema 做规范化摘要。Prompt 或 Tool 定义变化会自动生成新签名。

`routingKey` 将 `conversationKey` 与 `stablePrefixKey` 再次摘要。OpenAI、xAI 和 Generic 策略发送的 `prompt_cache_key` 都使用该字段。`prompt_cache_key` 是路由提示，不会让不同输入前缀变成相同前缀。

当前 Key 版本为 `v4`。修改序列化规则、安全隔离边界或稳定前缀组成时必须升级版本。

## 6. Responses 状态链

启用 `responses_state_mode=previous_response_id` 且 `disable_response_storage=false` 后，首轮发送完整历史、`store=true` 和会话 `prompt_cache_key`，但不发送 `previous_response_id`。成功后，Adapter 从普通响应 `id` 或流式 `response.completed.response.id` 获取 Provider Response ID。

后续请求发送 `previous_response_id`，input 仅包含上一个 Provider Response 之后新增的用户消息、`function_call_output` 和新的运行时上下文。领域请求同时保留完整 `messages` 与增量 `continuationMessages`，双轨数据只用于兼容性回退，不会同时发送。

每次成功响应都把 Response ID、Provider、Model、状态签名和更新时间写入 `ai_agent_conversation`。新 Run 只有在 Provider、Model 和 `routingKey` 全部一致时才复用状态。切换模型、修改 Prompt、变更 Tool Schema 或升级 Key 版本都会自然失效。

OpenAI 官方说明，使用 `previous_response_id` 时，链中此前输入仍会作为输入 Token 计费。状态链的价值是协议级连续性和正确复用 Provider 内部状态，不能描述成“历史 Token 不再计费”。

## 7. 全量历史回退

OpenAI-compatible 网关不一定实现 Responses 存储。当状态续接返回 HTTP `400`、`404`、`409` 或 `422` 时，Adapter 会立即使用完整 `messages` 重试并移除 `previous_response_id`。成功后保存新的 Response ID，遥测标记 `STATELESS_FALLBACK`。

`5xx`、网络断开和限流仍由既有重试策略处理。流式状态回退当前通过非流式完整请求完成，最终答案仍返回，但不会伪造已丢失的 Delta。

如果模型返回文本形式的伪 Tool Call，领域层需要合成为结构化 Tool Call，该输出不是 Provider 原生 `function_call`。此时主动清空 Response ID，下一轮使用完整历史，避免绑定无效 Call ID。

## 8. Provider 适配

OpenAI、xAI/Grok 和 Generic OpenAI-compatible 策略都使用会话分区后的 `routingKey`。专用策略负责 Provider Header/Body 差异，Generic 策略发送通用 `prompt_cache_key`。Usage 兼容解析 `prompt_tokens_details`、`input_tokens_details`、`cached_tokens`、cache read/write 别名。

状态链由 Responses Adapter 和模型绑定参数控制；缓存策略只负责缓存路由与 usage 字段差异。未知网关通过完整历史回退保持正确性。

## 9. 逐轮 CACHE_USAGE 遥测

`ai_agent_run_event.details_json` 为每次模型调用写入 `CACHE_USAGE`，字段包括 turn、input/cached/write tokens、命中率、Cache Key 哈希、稳定前缀哈希、请求 input 哈希、input item 数量、continuation mode、是否启用存储、是否回退，以及前后 Response ID 哈希。

事件不保存原始 Prompt、用户标识、会话号、Tool Result、API Key 或原始 Response ID。请求 input、Cache Key 和 Response ID 只记录 SHA-256，并继续经过 `AgentSensitiveDataPolicy.redactJson`。

## 10. 排障 SQL

查看最近逐轮命中：

```sql
SELECT run_no, sequence_no,
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

检查绑定：

```sql
SELECT capability, provider_code, model_override, params
FROM cynexus_appl_ai.ai_usage_bindings
WHERE capability = 'autonomous_agent';
```

期望包含 `wire_api=responses`、`responses_state_mode=previous_response_id`、`disable_response_storage=false` 和 `prompt_cache_retention=24h`。

检查会话状态：

```sql
SELECT conversation_no, provider_state_provider_code, provider_state_model_code,
       provider_state_signature IS NOT NULL AS has_signature,
       provider_response_id IS NOT NULL AS has_response_state,
       provider_state_updated_at
FROM cynexus_appl_ai.ai_agent_conversation
ORDER BY updated_at DESC
LIMIT 20;
```

不要把原始 `provider_response_id` 输出到普通日志或前端。

## 11. 成功判据

第一轮通常未命中或只命中共享静态前缀；第二轮开始应出现 `STATEFUL`。`cachedInputTokens` 不应机械固定在 `6,656`，在 Provider 支持且历史增长时应扩大。若一直是 `STATELESS_FALLBACK`，说明网关没有实现或透传 Responses 状态字段。

Codex 级别的大缓存数字来自很长且稳定的系统指令、工具定义、工作区上下文和会话历史。应用无法通过一个 Key 凭空制造同等命中；稳定前缀必须足够大且一致，Provider 也必须支持对应模型。

## 12. 安全与治理

Provider Response ID 存放在会话专用列，不暴露到 Agent API DTO。Tool 权限、租户隔离和实时查询不会因 Prompt Cache 被跳过。状态签名包含 Provider、Model、Prompt 和 Tool 协议。Response ID 失效只影响性能，完整历史回退保证正确性。

## 13. 测试清单

- Tool 顺序变化不改变稳定前缀；
- 会话变化改变 `routingKey`，但相同 Prompt/Tool 保持同一 `stablePrefixKey`；
- Prompt 或 Schema 变化会使状态失效；
- 首轮 `store=true` 且无 `previous_response_id`；
- 后续仅发送增量 Tool Result；
- 普通和流式响应都解析 Response ID；
- 状态错误触发完整历史回退；
- `CACHE_USAGE` 不包含原始标识。

## 14. 发布与回滚

先执行状态/遥测结构迁移和本知识库迁移，再发布 AI Release 镜像，最后运行多轮 Tool 场景检查逐轮事件。紧急降级可删除 `responses_state_mode` 或设置 `disable_response_storage=true`；代码将回到完整历史请求。若网关拒绝 `prompt_cache_retention`，从绑定 params 删除该字段即可，不需要回滚表结构。

## 15. 参考资料

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- OpenAI Conversation State：<https://developers.openai.com/api/docs/guides/conversation-state>
- xAI Prompt Caching：<https://docs.x.ai/developers/advanced-api-usage/prompt-caching>
- Anthropic Prompt Caching：<https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching>
- Gemini Context Caching：<https://ai.google.dev/gemini-api/docs/caching>
- vLLM Automatic Prefix Caching：<https://docs.vllm.ai/en/latest/features/automatic_prefix_caching/>
$doc$;
BEGIN
    INSERT INTO cynexus_appl_knowledge_base.kb_document
        (id, title, description, content, content_format, status, extra)
    VALUES
        (v_document_id,
         'Agent Prompt Cache 企业级架构与实现指南',
         'Cynexus Agent 会话分区缓存、Responses 状态链、兼容回退与逐轮遥测中文指南。',
         document_content,
         'markdown',
         'published',
         jsonb_build_object(
             'documentKey', 'agent_prompt_cache_enterprise_architecture',
             'sourcePath', 'docs/knowledge-base/agent-prompt-cache-enterprise-architecture.md',
             'documentVersion', 'v3-zh',
             'language', 'zh-CN',
             'tags', jsonb_build_array('ai-agent', 'prompt-cache', 'responses-api', 'architecture', '中文')))
    ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        description = EXCLUDED.description,
        content = EXCLUDED.content,
        content_format = EXCLUDED.content_format,
        status = EXCLUDED.status,
        extra = EXCLUDED.extra,
        updated_at = now(),
        deleted_at = NULL;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk
     WHERE kb_document_chunk.document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task
     WHERE kb_document_index_task.document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v3_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\\n\\s*\\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607261000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\\n', 1), E'^#+\\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v3-zh')
      FROM agent_prompt_cache_kb_v3_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607262000 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v3_blocks),
               'documentVersion', 'v3-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
