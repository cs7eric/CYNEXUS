\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000320,
    'market_indicator_replay_explanation',
    'Market indicator replay explanation',
    'market_indicator_replay_explanation',
    'v1',
    'active',
    '你是 Cynexus A 股盘后复盘解释器。只能使用输入中的确定性证据包解释一个板块 Episode；不得补充外部事实，不得修改评分、时间、收益、覆盖率、数据质量或事件顺序，不得把相关性说成确定因果。必须区分支持证据、反证、失效条件和不确定性。严禁输出买入、卖出、目标价、仓位、收益承诺或自动交易指令。只输出简体中文 JSON，不要 Markdown、思考过程或额外文字。',
    '请解释以下确定性复盘证据包，说明板块为何被确认、哪些证据支持、哪些证据削弱，以及什么条件会使结论失效：\n{{evidence}}',
    '{"type":"object","required":["summary","drivers","counterEvidence","failureConditions","uncertainties","evidenceRefs"],"properties":{"summary":{"type":"string"},"drivers":{"type":"array","items":{"type":"string"}},"counterEvidence":{"type":"array","items":{"type":"string"}},"failureConditions":{"type":"array","items":{"type":"string"}},"uncertainties":{"type":"array","items":{"type":"string"}},"evidenceRefs":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '["evidence"]'::jsonb,
    '["market","indicator","replay","json-only","no-advice"]'::jsonb,
    '{"capability":"market_indicator_replay_explanation","input":"one bounded Episode evidence packet","deterministic_scores_are_immutable":true}'::jsonb,
    '只解释已计算的复盘证据；AI 不参与排名、评分、时间对齐或交易决策。'
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
     ORDER BY CASE code WHEN 'custom' THEN 1 WHEN 'openai' THEN 2 WHEN 'custom-yancc' THEN 3 WHEN 'deepseek' THEN 4 ELSE 9 END, id
     LIMIT 1
), prompt AS (
    SELECT id FROM cynexus_appl_ai.ai_prompt
     WHERE prompt_key = 'market_indicator_replay_explanation' AND prompt_version = 'v1' AND status = 'active' AND deleted_at IS NULL
     LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT 910002000000000020,
       'market_indicator_replay_explanation',
       'Market indicator replay explanation',
       provider.id,
       NULLIF(provider.default_model, ''),
       TRUE,
       '{"prompt_version":"v1","fallback_chain":[]}'::jsonb,
       'User-triggered explanation over bounded deterministic market-indicator evidence; no trading advice.',
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

COMMIT;
