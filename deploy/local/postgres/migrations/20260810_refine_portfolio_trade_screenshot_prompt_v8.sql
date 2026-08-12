\set ON_ERROR_STOP on

BEGIN;

WITH base AS (
    SELECT *
      FROM cynexus_appl_ai.ai_prompt
     WHERE prompt_key = 'portfolio_trade_screenshot_parse'
       AND prompt_version = 'v7'
       AND status = 'active'
       AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
)
SELECT 910003000000000314,
       base.prompt_key, base.name, base.scenario_code, 'v8', 'active',
       base.system_prompt || E'\n成交时间必须来自截图中可见的成交日期/时间字段：不要使用图片上传时间、任务创建时间、AI处理时间、当前时间或任何系统时间填充 tradedAt。页面顶部、标题栏、筛选条件或表头中的共享日期必须应用到每一行；若只能读到时分秒，tradedAt 留为 null，不要伪造日期。',
       base.user_prompt_template || E'\n请逐行优先识别成交时间/成交日期列；不要把入库时间当成交时间。共享日期要应用到只有时间的行。',
       base.output_schema, base.variables,
       base.tags || '["trade-time-provenance"]'::jsonb,
       base.extra || '{}'::jsonb,
       base.remark || '成交时间必须可追溯到截图，不得使用系统时间。'
  FROM base
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    status = EXCLUDED.status, system_prompt = EXCLUDED.system_prompt,
    user_prompt_template = EXCLUDED.user_prompt_template, output_schema = EXCLUDED.output_schema,
    variables = EXCLUDED.variables, tags = EXCLUDED.tags, extra = EXCLUDED.extra,
    remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v8"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v8'
   AND prompt.status = 'active' AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
