\set ON_ERROR_STOP on

BEGIN;

-- Media transcription must use a real configured provider. Without this binding,
-- the routing fallback selects the generic mock adapter and stores a knowledge-base JSON envelope as polished text.
WITH custom AS (
    SELECT id
    FROM cynexus_appl_ai.ai_providers
    WHERE code = 'custom-yancc'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
),
prompt AS (
    SELECT id
    FROM cynexus_appl_ai.ai_prompt
    WHERE prompt_key = 'mp4_text_polish'
      AND prompt_version = 'legacy-v1'
      AND status = 'active'
      AND deleted_at IS NULL
    ORDER BY id DESC
    LIMIT 1
)
INSERT INTO cynexus_appl_ai.ai_usage_bindings (
    id, capability, label, provider_id, model_override, is_enabled, params, remark, prompt_id
)
SELECT
    910002000000000009,
    'text_polish',
    'MP4 transcription polish',
    deepseek.id,
    'auto',
    TRUE,
    '{}'::jsonb,
    'Media transcription capabilities use the configured yancc provider; do not fall back to the generic mock document generator.',
    prompt.id
FROM custom
LEFT JOIN prompt ON TRUE
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
SET provider_id = provider.id,
    model_override = 'auto',
    is_enabled = TRUE,
    updated_at = now(),
    deleted_at = NULL
FROM cynexus_appl_ai.ai_providers provider
WHERE binding.capability IN ('text_summary', 'post_metadata')
  AND provider.code = 'custom-yancc'
  AND provider.deleted_at IS NULL
  AND binding.deleted_at IS NULL;

COMMIT;
