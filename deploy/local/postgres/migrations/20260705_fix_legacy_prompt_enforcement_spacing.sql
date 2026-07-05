\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_ai.ai_prompt
SET system_prompt = replace(system_prompt, '}【输出硬约束补充】', '}' || chr(10) || chr(10) || '【输出硬约束补充】'),
    updated_at = now()
WHERE prompt_key = 'auction_analysis'
  AND prompt_version = 'legacy-v1'
  AND system_prompt LIKE '%}【输出硬约束补充】%';

UPDATE cynexus_appl_ai.ai_prompt
SET system_prompt = replace(system_prompt, '}【输出硬约束】', '}' || chr(10) || chr(10) || '【输出硬约束】'),
    updated_at = now()
WHERE prompt_key IN ('application_recent30_short_term_daily', 'application_analysis_annotation')
  AND prompt_version = 'legacy-v1'
  AND system_prompt LIKE '%}【输出硬约束】%';

COMMIT;
