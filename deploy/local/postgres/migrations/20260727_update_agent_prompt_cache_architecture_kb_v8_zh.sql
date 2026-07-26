DO $$
DECLARE
    v_document_id bigint := -202607260001;
    v_appendix text := $appendix$

## 23. v8：动态 system runtime context 才是 6,656 平台的根因

`1.1.296-260727-RELEASE` 使用会话独占 `prompt_cache_key` 运行真实 Agent `AGR-84dd6283612c`，第 2 至第 7 轮仍全部只命中 `6,656`。这证明 v7 的“共享 Key 是最终根因”假设不成立；会话隔离 Key 保留用于避免跨会话争用，但它不能单独修复缓存平台。

进一步使用真实 22 个 Tool、完整 guidance、相同 `gpt-5.4-mini`、相同 Responses 参数和相同回放结构做单变量 A/B，只改变 runtime context 的消息角色：

```text
runtime role=system: turn 1 = 0/11,190; turn 2 = 6,656/11,237
runtime role=user:   turn 1 = 0/11,178; turn 2 = 10,752/11,203
```

这个 A/B 精确复现了真实 Agent 的固定平台。每轮变化的 `system` runtime context 会参与 Provider 的特殊指令前缀处理；即使请求 JSON 数组中的旧 item 逐项一致，Provider 内部规范化后的有效前缀仍会在 Tool Schema 和历史之前分叉。因此 input item SHA-256 一致只能证明客户端回放稳定，不能证明 Provider 内部指令前缀稳定。

v8 将服务端生成的 runtime context 作为尾部 `user` 消息发送。稳定 System Prompt 已声明 runtime context 是每轮权威值，所以当前日期、工具预算、已用工具数和强制收敛语义保持不变。历史 runtime context 同样按原始顺序以 `user` 消息回放；Chat Completions 也补齐历史 runtime context。缓存 Key 升级为 v7，避免复用旧角色布局预热出的缓存分支。

真实验收必须确认：runtime context item kind 为 `user`；第 2 轮单次 cached input 超过 `6,656`；后续轮次随历史增长；同 conversation 的下一 Run 继续使用同一 Key 并复用历史。
$appendix$;
    document_content text;
BEGIN
    UPDATE cynexus_appl_knowledge_base.kb_document
       SET content = CASE
               WHEN content LIKE '%## 23. v8：动态 system runtime context 才是 6,656 平台的根因%'
                   THEN content
               ELSE content || v_appendix
           END,
           description = 'Cynexus Agent 基于真实 Provider A/B 的 Responses 无状态回放、runtime context 角色、会话缓存、加密 reasoning 与运维指南。',
           extra = COALESCE(extra, '{}'::jsonb)
                   || jsonb_build_object('documentVersion', 'v8-zh'),
           updated_at = now(),
           deleted_at = NULL
     WHERE id = v_document_id;

    SELECT content INTO document_content
      FROM cynexus_appl_knowledge_base.kb_document
     WHERE id = v_document_id;

    IF document_content IS NULL THEN
        RAISE EXCEPTION 'Prompt cache knowledge document % is missing; apply v7 migration first', v_document_id;
    END IF;

    DELETE FROM cynexus_appl_knowledge_base.kb_document_chunk WHERE document_id = v_document_id;
    DELETE FROM cynexus_appl_knowledge_base.kb_document_index_task WHERE document_id = v_document_id;

    CREATE TEMP TABLE agent_prompt_cache_kb_v8_blocks ON COMMIT DROP AS
    SELECT row_number() OVER (ORDER BY ordinal)::integer - 1 AS chunk_index,
           trim(block) AS content
      FROM regexp_split_to_table(document_content, E'\n\s*\n') WITH ORDINALITY AS blocks(block, ordinal)
     WHERE trim(block) <> '';

    INSERT INTO cynexus_appl_knowledge_base.kb_document_chunk
        (id, document_id, chunk_index, heading, content, content_hash, token_estimate, embedding_status, extra)
    SELECT -202607279000 - chunk_index,
           v_document_id,
           chunk_index,
           left(nullif(regexp_replace(split_part(content, E'\n', 1), E'^#+\s*', ''), ''), 256),
           content,
           md5(content),
           GREATEST(1, length(content) / 4),
           'pending',
           jsonb_build_object('source', 'markdown', 'documentVersion', 'v8-zh')
      FROM agent_prompt_cache_kb_v8_blocks;

    INSERT INTO cynexus_appl_knowledge_base.kb_document_index_task
        (id, document_id, target, status, payload)
    SELECT -202607279500 - row_number() OVER (ORDER BY target)::bigint,
           v_document_id,
           target,
           'pending',
           jsonb_build_object(
               'chunkCount', (SELECT count(*) FROM agent_prompt_cache_kb_v8_blocks),
               'documentVersion', 'v8-zh')
      FROM (VALUES ('pgvector'), ('elasticsearch')) AS targets(target);
END $$;
