\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000305,
    'market_futures_position_analysis',
    'Futures Position Structure Analysis',
    'market_futures_position',
    'v1',
    'active',
    '你是 Cynexus 的期货会员持仓结构研究助手，只分析 IF、IH、IM、IC 四个中金所股指期货品种。你只能使用输入中的当前快照、历史序列和会员排名，不得补充外部行情、新闻或未经输入支持的因果关系。你的职责是解释事实：多空结构、历史位置、变化持续性、会员增减仓证据和需要继续观察的条件。严禁输出买入、卖出、做多、做空、止损、目标价、仓位或收益预测。不要给品种打分，也不要使用主力、庄家等不可验证的词。只输出简体中文 JSON，不要输出 Markdown、代码围栏、思考过程或 JSON 之外的文字。JSON 必须包含 analysis_result 对象，且对象必须使用英文键 headline、structure、evidence、trend、risk、watch、data_quality、disclaimer；evidence 和 watch 必须是字符串数组，其他字段必须是字符串。',
    '请基于以下持仓数据输出结构化分析，只返回根字段为 analysis_result 的 JSON，不要改名、遗漏或嵌套这些字段：\n{{analysis_input}}',
    '{"type":"object","required":["analysis_result"],"properties":{"analysis_result":{"type":"object","required":["headline","structure","evidence","trend","risk","watch","data_quality","disclaimer"]}}}'::jsonb,
    '["analysis_input"]'::jsonb,
    '["market","futures-position","position-structure","index-futures","json-only"]'::jsonb,
    '{"capability":"market.futures_position_analysis","scope":"IF,IH,IM,IC","source":"futures-position"}'::jsonb,
    '期货持仓页面的事实型结构分析 prompt，不输出交易建议。'
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
    WHERE is_enabled
      AND deleted_at IS NULL
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
    WHERE prompt_key = 'market_futures_position_analysis'
      AND prompt_version = 'v1'
      AND status = 'active'
      AND deleted_at IS NULL
    LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT 910002000000000011,
       'market.futures_position_analysis',
       'Futures Position Structure Analysis',
       provider.id,
       NULLIF(provider.default_model, ''),
       TRUE,
       '{"prompt_version":"v1","fallback_chain":[]}'::jsonb,
       'Factual IF/IH/IM/IC futures position analysis. No trading recommendation.',
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
WHERE binding.capability = 'market.futures_position_analysis'
  AND prompt.prompt_key = 'market_futures_position_analysis'
  AND prompt.prompt_version = 'v1'
  AND prompt.status = 'active'
  AND prompt.deleted_at IS NULL
  AND binding.deleted_at IS NULL;

COMMIT;
