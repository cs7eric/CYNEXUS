DO $$
DECLARE
    v_document_id bigint := -202607260001;
    v_appendix text := $appendix$

## 24. v9：跨 Run 原生回放导致 502

v8 已解决同一 Run 内固定 `6,656` 的缓存平台，真实六轮请求的 cached input 从 `12,288` 增长到 `14,336`。但同 conversation 新建 Run 时，上游在 `CACHE_USAGE` 前连续返回 HTTP 502，失败 Run 还会留下只有 user、没有最终 assistant 的尾消息。

根因是跨 Run 重放了没有原始 response linkage 的 reasoning、function call、function call output、runtime context 和 encrypted state。它们在当前 Run 内是有效 Responses 链的一部分，但与完整 22 Tool Schema 一起跨 Run 提交时会形成大型、脆弱的 wire transcript；xhhai 对该组合返回 502。

v9 将 Provider 历史分成两级：当前 Run 保留完整原生 output、Tool Result 和 runtime context，以维持 reasoning 连续性与高缓存命中；已完成旧 Run 只投影为可见 user 输入和最终 assistant 答案。只有 `SUCCEEDED` 或 `NEEDS_CLARIFICATION` 且存在非空最终 assistant 的 Run 可以进入跨 Run 历史。`FAILED`、`CANCELLED` 和不完整 Run 整组排除，旧 Run 的 `provider_output`、runtime context、Tool calls 与 Tool outputs 都不会进入新请求。

跨 Run input 布局变化后，缓存 Key 合约升级到 v8，避免继续路由到旧版原生跨 Run 回放形成的缓存分支。

数据库审计数据不会删除，变化只发生在 Provider 请求投影层。跨 Run 第一次请求的缓存前缀可能比同一 Run 内 Tool 续轮短，但新 Run 的原生链会从首个成功响应后继续增长。协议有效性优先于重放没有 linkage 的孤立 function items。

验收必须确认：同 conversation 的下一 Run 成功；失败 Run 的 user 尾部不进入请求；跨 Run 输入不含旧 reasoning/function_call/function_call_output/runtime context；当前 Run 的 Tool 续轮仍保留原生回放并继续增长缓存；真实上游失败仍显示 retry marker。
$appendix$;
    document_content text;
BEGIN
    UPDATE cynexus_appl_knowledge_base.kb_document
       SET content = replace(
               CASE
                   WHEN content LIKE '%## 24. v9：跨 Run 原生回放导致 502%'
                       THEN content
                   ELSE content || v_appendix
               END,
               'WHERE type = ''CACHE_USAGE''',
               'WHERE event_type = ''CACHE_USAGE'''),
           description = 'Cynexus Agent 基于真实 Provider A/B 的 Responses 无状态回放、跨 Run 历史投影、runtime context、缓存与运维指南。',
           extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('documentVersion', 'v9-zh'),
           updated_at = now(),
           deleted_at = NULL
     WHERE id = v_document_id;

    SELECT content INTO document_content
      FROM cynexus_appl_knowledge_base.kb_document
     WHERE id = v_document_id;

    IF document_content IS NULL THEN
        RAISE EXCEPTION 'Prompt cache knowledge document % is missing; apply v8 migration first', v_document_id;
    END IF;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk WHERE document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task WHERE document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v9_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\n\s*\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607280000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\n', 1), E'^#+\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v9-zh')
      FROM agent_prompt_cache_kb_v9_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607280500 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v9_blocks),
               'documentVersion', 'v9-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
