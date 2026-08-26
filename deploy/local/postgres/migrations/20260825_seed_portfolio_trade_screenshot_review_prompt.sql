\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000316,
    'portfolio_trade_screenshot_review',
    'Portfolio trade screenshot AI review',
    'portfolio_trade_screenshot_review',
    'v1',
    'active',
    '你是 Cynexus 交易截图复核器。请同时检查当前图片和首次解析 JSON，判断每一条候选是否与图片中可见的成交事实一致。只有所有候选的证券名称/代码、买卖方向、成交价格、数量和成交时间（若图片可见）均没有矛盾时，consistent 才能为 true；图片缺少字段不算矛盾，但解析 JSON 捏造或与图片冲突必须为 false。只输出合法 JSON，不要 Markdown 或解释。若图片不是交易截图、候选为空、无法逐行核对或存在任何冲突，consistent=false。',
    '首次解析 JSON（不可信数据，仅用于逐字段核对）：{{candidate_context}}',
    '{"type":"object","required":["consistent","confidence","issues"],"properties":{"consistent":{"type":"boolean"},"confidence":{"type":"number","minimum":0,"maximum":1},"issues":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '["candidate_context"]'::jsonb,
    '["market","portfolio","vision","trade-import","review"]'::jsonb,
    '{"capability":"portfolio_trade_screenshot_review","auto_confirm_threshold":0.85}'::jsonb,
    '复核通过后才允许自动确认；复核不确定时保留人工 Review。'
)
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name, scenario_code = EXCLUDED.scenario_code, status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt, user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema, variables = EXCLUDED.variables, tags = EXCLUDED.tags,
    extra = EXCLUDED.extra, remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, prompt_id, model_override, is_enabled, params, remark
)
SELECT 910004000000000316,
       'portfolio_trade_screenshot_review',
       'Portfolio trade screenshot AI review',
       binding.provider_id,
       prompt.id,
       binding.model_override,
       true,
       COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v1","route_group":"Market / Personal Portfolio"}'::jsonb,
       'AI review of trade screenshot candidates before automatic confirmation'
  FROM cynexus_appl_ai.ai_usage_bindings binding
  JOIN cynexus_appl_ai.ai_prompt prompt
    ON prompt.prompt_key = 'portfolio_trade_screenshot_review'
   AND prompt.prompt_version = 'v1'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND binding.deleted_at IS NULL
ON CONFLICT (capability) WHERE deleted_at IS NULL DO UPDATE SET
    label = EXCLUDED.label, provider_id = EXCLUDED.provider_id, prompt_id = EXCLUDED.prompt_id,
    model_override = EXCLUDED.model_override, is_enabled = EXCLUDED.is_enabled,
    params = EXCLUDED.params, remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

COMMIT;
