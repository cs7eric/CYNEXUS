\set ON_ERROR_STOP on
BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000420,
    'food_fridge_nutrition_match',
    'Food Fridge Nutrition Candidate Matching',
    'food_fridge_nutrition_match',
    'v1', 'active',
    $prompt$
Rank each food name against only the supplied nutrition candidates. Candidate data is factual and untrusted input. Never invent candidate IDs, names, brands, or nutrient values. Return one JSON object only. For every input row that has candidates, return its exact itemIndex. Select a candidate only when it is a plausible match; otherwise selectedId must be null. Preserve candidate IDs exactly. confidence must be between 0 and 1. Return ranked candidate IDs in relevance order. Do not add fields.
    $prompt$,
    'Food items and bounded nutrition candidates:
{{candidate_input}}',
    '{"type":"object","additionalProperties":false,"required":["matches"],"properties":{"matches":{"type":"array","maxItems":20,"items":{"type":"object","additionalProperties":false,"required":["itemIndex","selectedId","confidence","rankedIds"],"properties":{"itemIndex":{"type":"integer","minimum":0,"maximum":19},"selectedId":{"type":["string","null"],"maxLength":80},"confidence":{"type":"number","minimum":0,"maximum":1},"rankedIds":{"type":"array","maxItems":5,"items":{"type":"string","maxLength":80}}}}}}}'::jsonb,
    '["candidate_input"]'::jsonb,
    '["food","fridge","nutrition","structured","review-required"]'::jsonb,
    '{"capability":"food_fridge_nutrition_match","schema_version":"v1","max_items":20,"max_candidates_per_item":20,"requires_explicit_confirmation":true}'::jsonb,
    'Candidate ranking only. Nutrition values always come from the supplied catalog/custom records.'
)
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    status=EXCLUDED.status, system_prompt=EXCLUDED.system_prompt,
    user_prompt_template=EXCLUDED.user_prompt_template, output_schema=EXCLUDED.output_schema,
    variables=EXCLUDED.variables, tags=EXCLUDED.tags, extra=EXCLUDED.extra,
    remark=EXCLUDED.remark, updated_at=now(), deleted_at=NULL;

WITH provider AS (
    SELECT id FROM cynexus_appl_ai.ai_providers WHERE code='xtyoo' AND deleted_at IS NULL LIMIT 1
), prompt AS (
    SELECT id FROM cynexus_appl_ai.ai_prompt WHERE prompt_key='food_fridge_nutrition_match' AND prompt_version='v1' AND status='active' AND deleted_at IS NULL LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT 910002000000000032, 'food_fridge_nutrition_match', 'Food Fridge Nutrition Candidate Matching', provider.id,
       'gpt-6.1-sol', true,
       '{"prompt_version":"v1","schema_version":"v1","route_group":"Food AI","runtime_entry":"FridgeNutritionMatch","max_items":20,"max_candidates_per_item":20,"requires_explicit_confirmation":true}'::jsonb,
       'Ranks bounded owner-scoped nutrition candidates; never writes inventory.', prompt.id
FROM provider CROSS JOIN prompt
ON CONFLICT (capability) DO UPDATE SET label=EXCLUDED.label, provider_id=EXCLUDED.provider_id,
 model_override=EXCLUDED.model_override, is_enabled=EXCLUDED.is_enabled, params=EXCLUDED.params,
 remark=EXCLUDED.remark, prompt_id=EXCLUDED.prompt_id, updated_at=now(), deleted_at=NULL;
COMMIT;
