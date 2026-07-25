UPDATE cynexus_appl_ai.ai_usage_bindings
SET params = jsonb_set(COALESCE(params, '{}'::jsonb), '{reasoning_effort}', '"low"'::jsonb, true),
    updated_at = now()
WHERE capability = 'autonomous_agent'
  AND deleted_at IS NULL
  AND NOT (COALESCE(params, '{}'::jsonb) ? 'reasoning_effort');

COMMENT ON COLUMN cynexus_appl_ai.ai_usage_bindings.params IS
    'Capability-specific runtime parameters, including reasoning_effort for Agent routes.';
