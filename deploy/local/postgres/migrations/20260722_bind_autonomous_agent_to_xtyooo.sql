\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark
)
SELECT
    910002000000000010,
    'autonomous_agent',
    'Autonomous data Agent',
    provider.id,
    'gpt-5.4-mini',
    true,
    '{"tool_call_mode":"native_then_json_fallback","max_turns":8,"max_tool_calls":12}'::jsonb,
    'Bound to the xtyooo custom provider for bounded read-only Agent execution.'
FROM cynexus_appl_ai.ai_providers provider
WHERE provider.code = 'custom'
  AND provider.deleted_at IS NULL
ON CONFLICT (capability) DO NOTHING;

COMMIT;
