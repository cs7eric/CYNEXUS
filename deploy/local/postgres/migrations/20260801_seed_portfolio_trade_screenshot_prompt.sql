\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000306,
    'portfolio_trade_screenshot_parse',
    'Portfolio trade screenshot parsing',
    'portfolio_trade_screenshot_parse',
    'v1',
    'active',
    '你是 Cynexus 个人持仓交易截图解析器。只提取图片中明确可见的成交事实，不要猜测、补全或计算持仓收益。一张图片可能包含多笔交易，必须拆分为 candidates 数组。side 只能是 BUY 或 SELL；证券代码、成交日期、成交价格、成交数量任一缺失时返回 null 并加入 warnings。费用、证券名称、成交编号和成交时间只有在截图明确出现时填写；无法确认的值必须为 null。每个字段提供 confidence 和原始 evidence 文本。只输出合法 JSON，不要 Markdown、思考过程或额外文字。',
    '请解析当前上传的交易截图，只返回符合 schema 的 JSON。',
    '{"type":"object","required":["isTradeScreenshot","candidates","warnings"],"properties":{"isTradeScreenshot":{"type":"boolean"},"candidates":{"type":"array","items":{"type":"object","required":["rowNo","symbol","securityName","side","tradedAt","quantity","price","fee","externalTradeNo","confidence","evidence","warnings"],"properties":{"rowNo":{"type":"integer"},"symbol":{"type":["string","null"]},"securityName":{"type":["string","null"]},"side":{"type":["string","null"],"enum":["BUY","SELL",null]},"tradedAt":{"type":["string","null"]},"quantity":{"type":["string","null"]},"price":{"type":["string","null"]},"fee":{"type":["string","null"]},"externalTradeNo":{"type":["string","null"]},"confidence":{"type":"number"},"evidence":{"type":"object"},"warnings":{"type":"array","items":{"type":"string"}}}}},"warnings":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '[]'::jsonb,
    '["market","portfolio","vision","trade-import","json-only"]'::jsonb,
    '{"capability":"portfolio_trade_screenshot_parse","input":"one image","scope":"A-share long-only CNY trades"}'::jsonb,
    'AI 只产生候选交易，用户确认后才进入 Portfolio 账本。'
)
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

WITH provider AS (
    SELECT id, default_model
      FROM cynexus_appl_ai.ai_providers
     WHERE is_enabled AND deleted_at IS NULL
     ORDER BY CASE code
        WHEN 'custom' THEN 1
        WHEN 'openai' THEN 2
        WHEN 'custom-yancc' THEN 3
        WHEN 'deepseek' THEN 4
        ELSE 9
     END, id
     LIMIT 1
), prompt AS (
    SELECT id
      FROM cynexus_appl_ai.ai_prompt
     WHERE prompt_key = 'portfolio_trade_screenshot_parse'
       AND prompt_version = 'v1'
       AND status = 'active'
       AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT 910002000000000012,
       'portfolio_trade_screenshot_parse',
       'Portfolio trade screenshot parsing',
       provider.id,
       NULLIF(provider.default_model, ''),
       TRUE,
       '{"prompt_version":"v1","fallback_chain":[]}'::jsonb,
       'Vision parsing only; user confirmation is required before ledger entry.',
       prompt.id
  FROM provider CROSS JOIN prompt
ON CONFLICT (capability) DO UPDATE SET
    label = EXCLUDED.label,
    provider_id = EXCLUDED.provider_id,
    model_override = EXCLUDED.model_override,
    is_enabled = EXCLUDED.is_enabled,
    params = EXCLUDED.params,
    remark = EXCLUDED.remark,
    prompt_id = EXCLUDED.prompt_id,
    updated_at = now(),
    deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v1"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v1'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
