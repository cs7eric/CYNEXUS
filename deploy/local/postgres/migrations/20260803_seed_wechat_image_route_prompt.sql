\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000310,
    'wechat_image_route',
    'WeChat image route classification',
    'wechat_image_route',
    'v1',
    'active',
    '你是 Cynexus 微信图片路由分类器。只判断图片属于哪个后续处理路径，不要提取完整业务数据，不要记账，不要写入交易，不要给出 Markdown 或解释。当前支持的 route 只有 ACCOUNTING_RECEIPT、PORTFOLIO_TRADE、UNKNOWN。ACCOUNTING_RECEIPT 表示小票、支付账单、消费凭证或收入凭证；PORTFOLIO_TRADE 表示券商、银行或交易软件中的股票、ETF、基金成交记录或交易明细；无法可靠判断时返回 UNKNOWN。只依据图片中可见内容，不要仅凭颜色、应用图标或猜测分类。',
    '请对当前上传的微信图片进行路由分类，只返回符合 schema 的 JSON。',
    '{"type":"object","required":["route","confidence","reason","signals","warnings"],"properties":{"route":{"type":"string","enum":["ACCOUNTING_RECEIPT","PORTFOLIO_TRADE","UNKNOWN"]},"confidence":{"type":"number","minimum":0,"maximum":1},"reason":{"type":"string"},"signals":{"type":"array","items":{"type":"string"}},"warnings":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '[]'::jsonb,
    '["wechat","vision","routing","json-only"]'::jsonb,
    '{"capability":"wechat_image_route","vision":true,"route_codes":["ACCOUNTING_RECEIPT","PORTFOLIO_TRADE","UNKNOWN"]}'::jsonb,
    '只负责图片分类；后续业务识别与质量控制由对应的现有 AI 能力负责。'
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
     WHERE prompt_key = 'wechat_image_route'
       AND prompt_version = 'v1'
       AND status = 'active'
       AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT 910002000000000013,
       'wechat_image_route',
       'WeChat image route classification',
       provider.id,
       NULLIF(provider.default_model, ''),
       TRUE,
       '{"prompt_version":"v1","fallback_chain":[]}'::jsonb,
       'Classifies WeChat images only; downstream accounting and portfolio QC remain separate.',
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
 WHERE binding.capability = 'wechat_image_route'
   AND prompt.prompt_key = 'wechat_image_route'
   AND prompt.prompt_version = 'v1'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
