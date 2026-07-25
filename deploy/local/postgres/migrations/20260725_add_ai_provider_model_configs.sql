ALTER TABLE cynexus_appl_ai.ai_providers
    ADD COLUMN IF NOT EXISTS model_configs jsonb NOT NULL DEFAULT '[]'::jsonb;

-- Preserve existing providers while moving model-level pricing out of the provider-wide fallback columns.
UPDATE cynexus_appl_ai.ai_providers
SET model_configs = COALESCE((
    SELECT jsonb_agg(jsonb_build_object(
        'model', model_name,
        'input_price_per_million', input_price_per_million,
        'output_price_per_million', output_price_per_million
    ) ORDER BY ordinal)
    FROM jsonb_array_elements_text(CASE
        WHEN jsonb_typeof(models) = 'array' THEN models
        ELSE '[]'::jsonb
    END) WITH ORDINALITY AS model_rows(model_name, ordinal)
), '[]'::jsonb)
WHERE model_configs = '[]'::jsonb
  AND jsonb_typeof(models) = 'array';

COMMENT ON COLUMN cynexus_appl_ai.ai_providers.model_configs IS
    'Configured provider models with per-model input/output token prices.';

-- Agent Responses API reasoning is a provider runtime option. Keep the current Agent route at high effort.
UPDATE cynexus_appl_ai.ai_providers
SET extra = jsonb_set(COALESCE(extra, '{}'::jsonb), '{model_reasoning_effort}', '"high"'::jsonb, true),
    updated_at = now()
WHERE code = 'grok' AND deleted_at IS NULL;
