\set ON_ERROR_STOP on

BEGIN;

-- xhhai can return a gateway 502/524 before its configured 120-second
-- upstream boundary. Keep user-selected primary routes, but cap their local
-- wait and continue with the independently configured DeepSeek provider.
WITH fallback_provider AS (
    SELECT code, default_model
    FROM cynexus_appl_ai.ai_providers
    WHERE code = 'deepseek'
      AND is_enabled
      AND deleted_at IS NULL
    LIMIT 1
)
UPDATE cynexus_appl_ai.ai_usage_bindings binding
SET params = COALESCE(binding.params, '{}'::jsonb)
        || jsonb_build_object(
            'request_timeout_seconds', 100,
            'fallback_provider_code', fallback_provider.code,
            'fallback_model', COALESCE(NULLIF(fallback_provider.default_model, ''), 'deepseek-v4-flash')
        ),
    updated_at = now()
FROM fallback_provider
WHERE binding.capability IN ('stock_selection_ai_screening', 'stock_selection_ai_intent_planning')
  AND binding.is_enabled
  AND binding.deleted_at IS NULL;

COMMIT;
