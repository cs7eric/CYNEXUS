\set ON_ERROR_STOP on

BEGIN;

WITH base AS (
    SELECT *
      FROM cynexus_appl_ai.ai_prompt
     WHERE prompt_key = 'portfolio_trade_screenshot_parse'
       AND prompt_version = 'v8'
       AND status = 'active'
       AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
)
SELECT 910003000000000315,
       base.prompt_key, base.name, base.scenario_code, 'v9', 'active',
       base.system_prompt || E'\n分类兜底规则：只要图片中出现成交明细/历史成交/交易记录等表格，或出现证券名称/代码与买卖、价格、数量、金额、时间中的任意组合，必须将 isTradeScreenshot 设为 true，并为每一行生成 candidate；无法辨认的字段填 null 并写入 warnings。只有完全没有交易表格或交易字段证据（例如纯聊天、风景、账单或收据）时才设为 false 且 candidates 为空。不得因为代码、日期、手续费或部分字段缺失而返回 false。',
       base.user_prompt_template || E'\n即使截图模糊或字段不完整，只要能确认是成交表，就返回 isTradeScreenshot=true 和逐行 candidates，未知字段使用 null；不要返回“不是交易截图”来代替结构化结果。',
       base.output_schema, base.variables,
       base.tags || '["classification-fallback","partial-row-tolerance"]'::jsonb,
       COALESCE(base.extra, '{}'::jsonb) || '{"classification_rule":"trade-table evidence takes precedence over field completeness"}'::jsonb,
       COALESCE(base.remark, '') || ' 模糊或字段不完整的成交表仍须生成可复核候选。'
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
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v9"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v9'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
