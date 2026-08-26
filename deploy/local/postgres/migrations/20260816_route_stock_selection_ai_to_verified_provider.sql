\set ON_ERROR_STOP on

BEGIN;

-- The custom/gpt-5.4 route was verified against the configured account. Keep
-- xhhai as an independent fallback and retain a route list so a user-selected
-- xhhai primary can fall back to custom instead of retrying itself.
WITH custom_provider AS (
    SELECT id
    FROM cynexus_appl_ai.ai_providers
    WHERE code = 'custom'
      AND is_enabled
      AND deleted_at IS NULL
    LIMIT 1
)
UPDATE cynexus_appl_ai.ai_usage_bindings binding
SET provider_id = CASE
        WHEN COALESCE(binding.params, '{}'::jsonb)->>'stock_selection_route_version' IS DISTINCT FROM 'v2'
            THEN custom_provider.id
        ELSE binding.provider_id
    END,
    model_override = CASE
        WHEN COALESCE(binding.params, '{}'::jsonb)->>'stock_selection_route_version' IS DISTINCT FROM 'v2'
            THEN 'gpt-5.4'
        ELSE binding.model_override
    END,
    params = COALESCE(binding.params, '{}'::jsonb) ||
        '{"stock_selection_route_version":"v2","request_timeout_seconds":100,"fallback_provider_codes":["custom","xhhai"],"fallback_provider_models":{"custom":"gpt-5.4","xhhai":"gpt-5.4-mini"}}'::jsonb,
    updated_at = now()
FROM custom_provider
WHERE binding.capability IN ('stock_selection_ai_screening', 'stock_selection_ai_intent_planning')
  AND binding.is_enabled
  AND binding.deleted_at IS NULL;

COMMIT;
