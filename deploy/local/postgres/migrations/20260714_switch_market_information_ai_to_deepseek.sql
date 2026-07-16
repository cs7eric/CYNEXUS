\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_ai.ai_usage_bindings
SET provider_id = (
        SELECT id
        FROM cynexus_appl_ai.ai_providers
        WHERE code = 'deepseek'
          AND deleted_at IS NULL
        LIMIT 1
    ),
    model_override = 'deepseek-v4-flash',
    params = '{"prompt_version":"v3","schema_version":"v2"}'::jsonb,
    remark = 'DeepSeek V4 Flash primary provider for Market Information analysis.',
    is_enabled = true,
    updated_at = now(),
    deleted_at = NULL
WHERE capability IN (
    'market_information_event_analysis',
    'market_information_daily_brief'
)
  AND EXISTS (
      SELECT 1
      FROM cynexus_appl_ai.ai_providers
      WHERE code = 'deepseek'
        AND is_enabled = true
        AND deleted_at IS NULL
  );

DO $$
BEGIN
    IF (
        SELECT count(*)
        FROM cynexus_appl_ai.ai_usage_bindings binding
        JOIN cynexus_appl_ai.ai_providers provider ON provider.id = binding.provider_id
        WHERE binding.capability IN (
            'market_information_event_analysis',
            'market_information_daily_brief'
        )
          AND provider.code = 'deepseek'
          AND binding.model_override = 'deepseek-v4-flash'
          AND binding.is_enabled = true
          AND binding.deleted_at IS NULL
    ) <> 2 THEN
        RAISE EXCEPTION 'Unable to switch both Market Information AI bindings to DeepSeek V4 Flash';
    END IF;
END $$;

COMMIT;
