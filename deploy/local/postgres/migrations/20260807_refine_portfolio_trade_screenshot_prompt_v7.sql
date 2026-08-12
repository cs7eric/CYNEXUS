\set ON_ERROR_STOP on

BEGIN;

WITH base AS (
    SELECT *
      FROM cynexus_appl_ai.ai_prompt
     WHERE prompt_key = 'portfolio_trade_screenshot_parse'
       AND prompt_version = 'v6'
       AND status = 'active'
       AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
)
SELECT 910003000000000313,
       base.prompt_key,
       base.name,
       base.scenario_code,
       'v7',
       'active',
       base.system_prompt || E'\n手续费规则：fee 只能填写明确位于“手续费”或“佣金”列且与当前行对齐的数值。“成交额”“成交金额”“金额”“发生金额”列的数值绝对不能写入 fee；手续费列不存在、为空或无法确认时，fee 必须为 null。',
       base.user_prompt_template || E'\nfee 只来自明确可见且行列对齐的手续费/佣金字段，不能从成交额推导或复制；看不到手续费时返回 null。',
       base.output_schema,
       base.variables,
       base.tags || '["fee-column-protection"]'::jsonb,
       base.extra || '{"fee_rule":"never copy gross amount into fee"}'::jsonb,
       base.remark || ' 服务端会拒绝明显等于成交额的误识别手续费。'
  FROM base
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name,
    scenario_code = EXCLUDED.scenario_code,
    status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt,
    user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema,
    variables = EXCLUDED.variables,
    tags = EXCLUDED.tags,
    extra = EXCLUDED.extra,
    remark = EXCLUDED.remark,
    updated_at = now(),
    deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v7"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v7'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
