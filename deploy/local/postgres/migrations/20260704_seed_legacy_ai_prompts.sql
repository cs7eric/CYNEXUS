\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id,
    prompt_key,
    name,
    scenario_code,
    prompt_version,
    status,
    system_prompt,
    user_prompt_template,
    output_schema,
    variables,
    tags,
    extra,
    remark
)
SELECT
    910801000000000001,
    'mp4_text_polish',
    'MP4 转写文本润色',
    'text_polish',
    'legacy-v1',
    'active',
    $prompt$你是一个专业的A股投资顾问，擅长将口语化内容整理成专业、简洁的文字。

任务：将转写文本润色成专业、有条理的文章。

要求：
- 保持原意，去除重复和语气词
- 使用专业术语（K线、板块、量价配合、打板、首板等）
- 段落分明，逻辑清晰
- 不添加原文本中没有的内容
- 用中文输出$prompt$,
    $prompt$请润色以下转写文本：

{{text}}$prompt$,
    '{"type":"string"}'::jsonb,
    '["text"]'::jsonb,
    '["legacy","mp4","transcription","polish"]'::jsonb,
    '{"migrated_from":"E:/project-repo/mp4-to-word/polisher.py","legacy_method":"TextPolisher.polish","provider":"minimax","model":"MiniMax-M2.5","temperature":0.7}'::jsonb,
    '从旧 mp4-to-word TextPolisher.polish 迁移'
WHERE NOT EXISTS (
    SELECT 1
    FROM cynexus_appl_ai.ai_prompt
    WHERE prompt_key = 'mp4_text_polish'
      AND prompt_version = 'legacy-v1'
      AND deleted_at IS NULL
);

INSERT INTO cynexus_appl_ai.ai_prompt (
    id,
    prompt_key,
    name,
    scenario_code,
    prompt_version,
    status,
    system_prompt,
    user_prompt_template,
    output_schema,
    variables,
    tags,
    extra,
    remark
)
SELECT
    910801000000000002,
    'mp4_text_summary',
    'MP4 转写摘要提取',
    'text_summary',
    'legacy-v1',
    'active',
    $prompt$你是一个专业的A股市场分析师，擅长提炼核心内容并用专业术语总结。

请分析以下文本，提取关键信息：

输出格式（严格JSON）：
{
    "核心内容": "一段话概括核心内容（不超过50字）",
    "关键要点": ["要点1（不超过30字）", "要点2", "要点3"]
}

注意：
- 只返回纯 JSON，不要有其他文字
- 关键要点不超过5条$prompt$,
    $prompt${{text}}$prompt$,
    '{"type":"object","properties":{"核心内容":{"type":"string","maxLength":50},"关键要点":{"type":"array","maxItems":5,"items":{"type":"string","maxLength":30}}},"required":["核心内容","关键要点"],"additionalProperties":false}'::jsonb,
    '["text"]'::jsonb,
    '["legacy","mp4","transcription","summary"]'::jsonb,
    '{"migrated_from":"E:/project-repo/mp4-to-word/polisher.py","legacy_method":"TextPolisher.summarize","provider":"minimax","model":"MiniMax-M2.5","temperature":0.3}'::jsonb,
    '从旧 mp4-to-word TextPolisher.summarize 迁移'
WHERE NOT EXISTS (
    SELECT 1
    FROM cynexus_appl_ai.ai_prompt
    WHERE prompt_key = 'mp4_text_summary'
      AND prompt_version = 'legacy-v1'
      AND deleted_at IS NULL
);

COMMIT;
