DO $$
DECLARE
    v_document_id bigint := -202607260001;
    v_appendix text := $appendix$

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
$appendix$;
    document_content text;
BEGIN
    UPDATE cynexus_appl_knowledge_base.kb_document
       SET content = CASE
               WHEN content LIKE '%## 21. v6：HTTP SSE 与非流式 Responses 的实测差异%'
                   THEN content
               ELSE content || v_appendix
           END,
           description = 'Cynexus Agent 基于真实 Provider A/B 的 Responses 无状态回放、buffered HTTP、加密 reasoning、缓存遥测与运维指南。',
           extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('documentVersion', 'v6-zh'),
           updated_at = now(),
           deleted_at = NULL
     WHERE id = v_document_id;

    SELECT content
      INTO document_content
      FROM cynexus_appl_knowledge_base.kb_document
     WHERE id = v_document_id;

    IF document_content IS NULL THEN
        RAISE EXCEPTION 'Prompt cache knowledge document % is missing; apply v5 migration first', v_document_id;
    END IF;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk
     WHERE document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task
     WHERE document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v6_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\\n\\s*\\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607267000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\\n', 1), E'^#+\\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v6-zh')
      FROM agent_prompt_cache_kb_v6_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607268000 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v6_blocks),
               'documentVersion', 'v6-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
