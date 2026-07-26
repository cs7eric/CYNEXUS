DO $$
DECLARE
    v_document_id bigint := -202607260001;
    document_content text := $doc$# Agent Prompt Cache 企业级架构与实现指南

## 1. 文档目标

本文解释 Cynexus Agent 的 Prompt Cache 原理、历史请求为什么长期只命中约 `6.7K` Token，以及 v4 如何在 xhhai 当前无法使用 Responses WebSocket 和 HTTP `previous_response_id` 的条件下，通过 Responses HTTP 无状态原生回放提升缓存利用率。

Prompt Cache 缓存的是模型输入前缀的计算状态，不是最终答案。每轮仍会调用模型、校验 Tool 权限、查询实时数据并生成新答案。

## 2. OpenAI 官方缓存机制

缓存命中要求从第一个 Token 开始形成精确相同的前缀。稳定 System Prompt、示例和 Tool Schema 应放在前面，用户输入、时间、预算等动态内容应放在后面。消息、图片、Tool 定义和 Structured Output Schema 都参与匹配。

输入达到 `1,024` Token 后才具备缓存资格。`prompt_cache_key` 用于把共享长前缀的请求路由到更可能命中的缓存节点，但不会把不同 Prompt 变成相同 Prompt，也不会绕过精确前缀匹配。OpenAI 建议每个 Key 的总流量约为每分钟 `15` 个请求，高并发场景应稳定分片。

GPT-5.6 及后续模型使用 `prompt_cache_options` 和可选显式 breakpoint；较早模型使用 `prompt_cache_retention`。兼容网关可能不支持所有字段，必须按 Provider 实测能力降级。

`store=false` 只关闭 Response 对象持久化，不会关闭 Prompt Cache。无状态客户端必须保存并回放完整原生 output。

## 3. 固定 6.7K 的根因

历史数据：

```text
input : 15163, 25959, 27375, 38993
cached:     0,  6656,  6656,  6656
```

这说明只有稳定 System Prompt 和 Tool Schema 被复用。旧实现只保存可见 Assistant 文本、解析后的 Tool Call 和 Tool Result，丢失 Responses `output[]` 中的 reasoning、`encrypted_content`、原生 function call、Assistant `phase` 以及产生输出前紧邻的 runtime context。下一轮的线路前缀因此不能延续增长。

## 4. xhhai 实测能力

| 能力 | 结果 | v4 处理 |
| --- | --- | --- |
| HTTP `/v1/responses` | 成功 | 当前正式传输 |
| Responses WebSocket | Upgrade 后关闭 `1013 no available account` | 当前禁用 |
| HTTP `previous_response_id` | `400`，仅支持 WebSocket v2 | 不发送 |
| `store=false` 完整 output 回放 | 成功 | 当前连续性方案 |
| reasoning/function/tool output 回放 | 成功 | Tool 链采用 |

OpenAI 官方服务支持 HTTP 和 WebSocket continuation；xhhai 的限制是 Provider 兼容偏差，不是 Responses API 的通用限制。

## 5. v4 请求结构

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
  用户输入 + runtime-1 -> output-1[]
  回放 output-1[] + tool-result-1 + runtime-2 -> output-2[]
  回放 output-2[] + tool-result-2 + runtime-3 -> output-3[]
```

每轮发送完整无状态 input，但历史前缀保持一致。Provider 可以复用逐轮增长的稳定前缀。

## 6. PromptCacheContext v5

`conversationKey` 是 Key 版本、owner 和 conversation 的 SHA-256，不暴露原始租户或会话标识。

`stablePrefixKey` 对 capability、Provider、Model、DB 管理的稳定 System Prompt，以及按 Tool Code 排序后的 description、guidance、input/output Schema 做摘要。Prompt 或 Tool 协议变化会自然生成新身份。

`routingKey` 使用 `stablePrefixKey + deterministic conversation shard(0..15)`，再做 SHA-256 生成 `cynexus-agent-...`。相同稳定前缀可在最多 16 个分区中共享，兼顾跨会话复用和单 Key 流量限制。

## 7. 原生 output 回放顺序

第一轮请求示意：

```json
{
  "model": "gpt-5.4-mini",
  "store": false,
  "prompt_cache_key": "cynexus-agent-...",
  "input": [
    {"role": "system", "content": "稳定系统提示"},
    {"role": "user", "content": "用户问题"},
    {"role": "system", "content": "runtime-1"}
  ]
}
```

若返回 reasoning 和 function call，Tool 续轮必须按以下顺序发送：

```json
[
  {"role": "system", "content": "稳定系统提示"},
  {"role": "user", "content": "用户问题"},
  {"role": "system", "content": "runtime-1"},
  {"type": "reasoning", "encrypted_content": "opaque..."},
  {"type": "function_call", "id": "fc_1", "call_id": "call_1", "arguments": "{}"},
  {"type": "function_call_output", "call_id": "call_1", "output": "..."},
  {"role": "system", "content": "runtime-2"}
]
```

必须原样保存 reasoning、function call、Assistant message、`phase` 和未来 Provider opaque item，不能仅重建普通 Assistant 文本。

`runtimeContext` 是上一轮输出产生前最后一个 input item。跨 Run 若丢失它，精确前缀会从该位置断开。因此 Assistant 消息同时保存前置 runtime context 和该响应的完整 provider output。

## 8. 持久化字段

`cynexus_appl_ai.ai_agent_message` 新增：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| `runtime_context` | `text` | 产生该 Assistant 输出前的动态系统输入 |
| `provider_output` | `jsonb` | 完整 Responses `output[]` |

Infra Adapter 负责 Provider JSON 的提取、验证和回放；Domain 只负责顺序、生命周期与持久化。旧消息无原生 output 时继续使用可见文本兼容路径。只有具备原生 function call 的 Tool Result 才允许跨 Run 继续回放。

## 9. 三条响应路径

- 普通 HTTP 从根对象 `output` 捕获；
- SSE 从 `response.completed.response.output` 捕获；
- WebSocket 从 `response.completed.response.output` 捕获。

增量事件用于实时 UI，最终 completed 事件中的完整 output 是后续无状态回放的权威数据。

## 10. Reasoning 与 phase

OpenAI 官方建议在 Tool 链中回传自最后一次用户消息之后的全部 reasoning、function call 和 function call output。GPT-5.4/GPT-5.5 长 Tool 流程还应保留 Assistant `phase`：`commentary` 表示中间状态，`final_answer` 表示最终答案。丢失 phase 可能让模型把中间说明当作最终答案。

## 11. 安全边界

持久化前继续执行 `AgentSensitiveDataPolicy`：Tool arguments 的 API Key、token、email 等字段会脱敏；runtime context 中的敏感文本会脱敏；`encrypted_content` 作为 opaque reasoning 状态保留。同一 Run 内存回放保持无损，跨 Run 回放遵守数据库持久化安全策略。

不能为了提高缓存率把明文凭据写入会话表。

## 12. xhhai 绑定

```json
{
  "wire_api": "responses",
  "responses_transport": "http",
  "disable_response_storage": true,
  "prompt_cache_retention": "24h"
}
```

必须移除 `responses_state_mode`，并清空旧 xhhai `provider_response_id` 状态。只有 WebSocket v2 账号池经过独立探测和多 Tool 回归后，才能重新开启 state continuation。

## 13. CACHE_USAGE 遥测

第一轮无历史 output 时显示 `STATELESS`；只要本轮 input 回放了 Provider 原生 output，就显示 `STATELESS_REPLAY`。

重点字段包括 input/cached/write tokens、命中率、缓存 Key 哈希、稳定前缀哈希、请求 input 哈希、input item 数量和 continuation mode。遥测不保存原始 Prompt、Tool Result、API Key、Response ID 或用户标识。

查看逐轮缓存：

```sql
SELECT run_no, sequence_no,
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

检查原生 output 落库：

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

## 14. 成功判据

1. 第一轮为 `STATELESS`；
2. 第二个模型调用开始为 `STATELESS_REPLAY`；
3. Assistant 的 provider output 和 runtime context 持续落库；
4. 多个 Tool 往返后，cached tokens 突破旧的 `6,656` 平台；
5. input 增长时，缓存读 Token 在后续请求中逐步增长；
6. 不再尝试 xhhai WebSocket，也不发送 HTTP `previous_response_id`。

某轮刚生成的 output 不会在同一请求内立即成为缓存输入；它要在下一轮被回放后才可能写入更长缓存前缀，因此至少需要多个模型/Tool 往返观察增长。

## 15. 长会话 Compaction

OpenAI 提供 `context_management` 服务端 compaction 和独立 `/responses/compact`。两者返回 opaque compaction item，无状态客户端必须保存并回放。只有出现最新 compaction item 后，才可按官方规则丢弃更早历史。

xhhai compaction 能力尚未验证，v4 不默认发送相关字段。未来必须通过 Provider 能力开关、实测和回归后接入。

## 16. 发布与回滚

先执行 `20260726_add_agent_stateless_responses_replay.sql`，再执行本 v4 知识库迁移，发布同步版本 Release 镜像，最后创建新会话执行多个 Tool 往返并检查落库和 `CACHE_USAGE`。

紧急降级可保留数据库列并切换绑定。不要恢复 xhhai HTTP `previous_response_id`，除非 Provider 已明确修复并通过验证。

## 17. 官方参考

- OpenAI Prompt Caching：<https://developers.openai.com/api/docs/guides/prompt-caching>
- OpenAI Conversation State：<https://developers.openai.com/api/docs/guides/conversation-state>
- OpenAI Reasoning：<https://developers.openai.com/api/docs/guides/reasoning>
- OpenAI Compaction：<https://developers.openai.com/api/docs/guides/compaction>
- OpenAI Responses WebSocket：<https://developers.openai.com/api/docs/guides/websocket-mode>
$doc$;
BEGIN
    INSERT INTO cynexus_appl_knowledge_base.kb_document
        (id, title, description, content, content_format, status, extra)
    VALUES
        (v_document_id,
         'Agent Prompt Cache 企业级架构与实现指南',
         'Cynexus Agent 基于 OpenAI 官方指南的 Responses HTTP 无状态原生回放、缓存分片、遥测与运维指南。',
         document_content,
         'markdown',
         'published',
         jsonb_build_object(
             'documentKey', 'agent_prompt_cache_enterprise_architecture',
             'sourcePath', 'docs/knowledge-base/agent-prompt-cache-enterprise-architecture.md',
             'documentVersion', 'v4-zh',
             'language', 'zh-CN',
             'tags', jsonb_build_array('ai-agent', 'prompt-cache', 'responses-api', 'stateless-replay', 'architecture', '中文')))
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

    CREATE TEMP TABLE agent_prompt_cache_kb_v4_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\\n\\s*\\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607263000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\\n', 1), E'^#+\\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v4-zh')
      FROM agent_prompt_cache_kb_v4_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607264000 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v4_blocks),
               'documentVersion', 'v4-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
