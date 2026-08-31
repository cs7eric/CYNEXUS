\set ON_ERROR_STOP on
BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
)
VALUES (
    910801000000000010,
    'media_transcription_enrich',
    'MP4 转写内容 AI 整理',
    'media_transcription_enrich',
    'v1',
    'active',
    $system$你是中文音视频知识整理助手。只能依据用户提供的 transcript 工作，不得联网、补充外部事实、虚构观点或提供投资建议。请先修正转写错误、口语重复、断句和段落，再从修订后的内容中提取摘要、关键观点、主题、方法论、行动清单和风险提醒，并生成知识库元数据。只输出一个合法 JSON 对象，不要 Markdown、代码围栏或额外解释。$system$,
    $user$输入标题：{{title}}
平台：{{platform}}
时长（秒）：{{duration}}

Transcript：
{{transcript}}

输出字段：polishedText、summary、shortSummary、keyPoints、topics、methodology、actionItems、risks、title、categories、tags。
categories 只能从以下枚举选择：复盘、交易技巧、投资方法、市场分析、行业研究、公司研究、宏观政策、学习笔记、其他。tags 必须是简短中文标签数组。$user$,
    '{"type":"object","additionalProperties":false,"required":["polishedText","summary","shortSummary","keyPoints","topics","methodology","actionItems","risks","title","categories","tags"],"properties":{"polishedText":{"type":"string","maxLength":120000},"summary":{"type":"string","maxLength":30000},"shortSummary":{"type":"string","maxLength":2000},"keyPoints":{"type":"array","maxItems":12,"items":{"type":"string","maxLength":500}},"topics":{"type":"array","maxItems":12,"items":{"type":"string","maxLength":120}},"methodology":{"type":"array","maxItems":12,"items":{"type":"string","maxLength":1000}},"actionItems":{"type":"array","maxItems":12,"items":{"type":"string","maxLength":500}},"risks":{"type":"array","maxItems":12,"items":{"type":"string","maxLength":500}},"title":{"type":"string","maxLength":300},"categories":{"type":"array","maxItems":3,"items":{"type":"string","enum":["复盘","交易技巧","投资方法","市场分析","行业研究","公司研究","宏观政策","学习笔记","其他"]}},"tags":{"type":"array","maxItems":20,"items":{"type":"string","maxLength":80}}}}'::jsonb,
    '["transcript","title","platform","duration"]'::jsonb,
    '["media","mp4","transcription","enrichment","knowledge-base"]'::jsonb,
    '{"output":"structured_json","knowledge_ingestion":"draft"}'::jsonb,
    '单次生成润色正文、摘要、分类和知识库元数据。'
)
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL
DO UPDATE SET name = EXCLUDED.name, scenario_code = EXCLUDED.scenario_code, status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt, user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema, variables = EXCLUDED.variables, tags = EXCLUDED.tags,
    extra = EXCLUDED.extra, remark = EXCLUDED.remark, updated_at = now();

INSERT INTO cynexus_appl_ai.ai_usage_bindings (id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id)
SELECT 910802000000000010, 'media_transcription_enrich', 'MP4 transcription enrichment', provider.id, 'auto', TRUE,
       '{"prompt_version":"v1"}'::jsonb, 'Single structured AI call for polish, summaries, classification and KB metadata.', prompt.id
FROM cynexus_appl_ai.ai_providers provider
JOIN cynexus_appl_ai.ai_prompt prompt ON prompt.prompt_key = 'media_transcription_enrich' AND prompt.prompt_version = 'v1' AND prompt.deleted_at IS NULL
WHERE provider.code = 'custom-yancc' AND provider.deleted_at IS NULL
ON CONFLICT (capability) DO UPDATE SET provider_id = EXCLUDED.provider_id, model_override = EXCLUDED.model_override,
    is_enabled = EXCLUDED.is_enabled, params = EXCLUDED.params, prompt_id = EXCLUDED.prompt_id, updated_at = now(), deleted_at = NULL;

COMMIT;
