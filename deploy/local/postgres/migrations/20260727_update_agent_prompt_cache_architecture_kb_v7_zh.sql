DO $$
DECLARE
    v_document_id bigint := -202607260001;
    v_appendix text := $appendix$

## 22. v7：固定 6,656 Token 的最终根因与会话隔离修正

`1.1.295-260727-RELEASE` 对真实 Agent 请求增加了不含原文的边界遥测：每个 input item 的 SHA-256、累积前缀 SHA-256、字节数和 item kind，以及 Tool Schema 与请求选项哈希。真实 Run `AGR-d2627dcf79ab` 连续执行六个模型轮次，输入从 `12,294` 增长到 `19,556`，但每个后续请求都只缓存 `6,656` Token。

逐项对比确认：第 2 轮的 stable system、user、旧 runtime context 与第 1 轮完全一致；第 3 至第 6 轮也完整继承此前所有 reasoning、message、function_call、function_call_output 与 runtime context；Tool Schema 和请求选项始终一致。由此排除了历史丢失、JSON 排序、动态日期、reasoning effort 和 encrypted state 变化。

最终根因是 v5 routing key 只包含 `stablePrefixKey + shard(0..15)`。多个会话共用同一个显式 `prompt_cache_key`，而 xhhai 会按该 Key 隔离或固定缓存扩展路径。不同会话从首个用户输入开始分叉后，只剩稳定 System Prompt 与 Tool Schema 能共同命中，恰好形成 `6,656` 平台。

严格 A/B 进一步验证了这一点：使用一个独占显式 Key 时，三轮 cached input 为 `0 -> 7,552 -> 8,064`；完全不发送 Key 时三轮均为 `0`。因此正确修复不是删除 `prompt_cache_key`，而是给每个会话独立 Key。v6 Key 仍绑定 stable prefix，并加入不可逆的 conversation digest。代价是新会话第一轮不再借用其他会话的 6,656 静态缓存；收益是第二轮开始可以复用该会话完整且持续增长的历史，这对多轮 Agent 的总 Token 成本更重要。

验收必须同时检查：同一会话 Key 不变；不同会话 Key 不同；第 2 轮单次 cached input 明显超过 `6,656`；后续轮次随历史增长；跨 Run 继续同一 conversation 时仍使用同一 Key；UI retry marker 和上游 502 重试保持可见。
$appendix$;
    document_content text;
BEGIN
    UPDATE cynexus_appl_knowledge_base.kb_document
       SET content = CASE
               WHEN content LIKE '%## 22. v7：固定 6,656 Token 的最终根因与会话隔离修正%'
                   THEN content
               ELSE content || v_appendix
           END,
           description = 'Cynexus Agent 基于真实 Provider A/B 的 Responses 无状态回放、会话隔离缓存、加密 reasoning、缓存遥测与运维指南。',
           extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('documentVersion', 'v7-zh'),
           updated_at = now(),
           deleted_at = NULL
     WHERE id = v_document_id;

    SELECT content INTO document_content
      FROM cynexus_appl_knowledge_base.kb_document
     WHERE id = v_document_id;

    IF document_content IS NULL THEN
        RAISE EXCEPTION 'Prompt cache knowledge document % is missing; apply v6 migration first', v_document_id;
    END IF;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk WHERE document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task WHERE document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v7_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\\n\\s*\\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607277000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\\n', 1), E'^#+\\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v7-zh')
      FROM agent_prompt_cache_kb_v7_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607278000 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v7_blocks),
               'documentVersion', 'v7-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
