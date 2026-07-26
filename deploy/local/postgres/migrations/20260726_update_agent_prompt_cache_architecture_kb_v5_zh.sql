DO $$
DECLARE
    v_document_id bigint := -202607260001;
    v_appendix text := $appendix$

## 18. xhhai 加密 reasoning 返回差异与 v5 修正

部署 v4 后的真实 Agent Run 显示：第二、三轮已经进入 `STATELESS_REPLAY`，request routing key 和 stable prefix 保持一致，数据库也保存了完整 `provider_output`，但缓存仍固定为 `6,656` Token。检查原生 output 后发现，xhhai 返回的 reasoning item 只有 summary，没有无状态续传必需的 `encrypted_content`。

OpenAI Reasoning 指南要求无状态客户端保存并回放完整 output，包括 opaque 加密 reasoning 状态。官方当前服务在 `store=false` 时默认返回该字段，同时仍兼容显式请求：

```json
{
  "store": false,
  "include": ["reasoning.encrypted_content"]
}
```

对 xhhai 的直接探测确认：显式设置 `include` 后，reasoning item 会返回 `encrypted_content`，Assistant message 也会保留 `phase`。v5 因此在所有 Agent Responses 请求中发送该 include，并继续原样持久化和回放完整 `output[]`。

`encrypted_content` 对 Cynexus 是不解析、不修改的 opaque 字符串，不是 reasoning 明文。部署后必须新建会话重新预热；旧消息中缺失的加密状态无法补造。
$appendix$;
    document_content text;
BEGIN
    UPDATE cynexus_appl_knowledge_base.kb_document
       SET content = CASE
               WHEN content LIKE '%## 18. xhhai 加密 reasoning 返回差异与 v5 修正%'
                   THEN content
               ELSE content || v_appendix
           END,
           description = 'Cynexus Agent 基于 OpenAI 官方指南的 Responses HTTP 无状态原生回放、加密 reasoning、缓存分片、遥测与运维指南。',
           extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('documentVersion', 'v5-zh'),
           updated_at = now(),
           deleted_at = NULL
     WHERE id = v_document_id;

    SELECT content
      INTO document_content
      FROM cynexus_appl_knowledge_base.kb_document
     WHERE id = v_document_id;

    IF document_content IS NULL THEN
        RAISE EXCEPTION 'Prompt cache knowledge document % is missing; apply v4 migration first', v_document_id;
    END IF;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk
     WHERE document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task
     WHERE document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v5_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\\n\\s*\\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607265000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\\n', 1), E'^#+\\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v5-zh')
      FROM agent_prompt_cache_kb_v5_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607266000 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v5_blocks),
               'documentVersion', 'v5-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
