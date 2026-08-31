\set ON_ERROR_STOP on
BEGIN;

UPDATE cynexus_appl_ai.ai_prompt
SET user_prompt_template = replace(user_prompt_template,
    'tags 必须是简短中文标签数组。',
    'title 必须是概括主题的短标题，控制在 30 个中文字符以内，不得复述正文。tags 必须是简短中文标签数组。'),
    output_schema = jsonb_set(output_schema, '{properties,title,maxLength}', '80'::jsonb),
    updated_at = now()
WHERE prompt_key = 'media_transcription_enrich'
  AND prompt_version = 'v1'
  AND deleted_at IS NULL;

COMMIT;
