\set ON_ERROR_STOP on
BEGIN;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
SET provider_id = provider.id,
    model_override = 'grok-4.5',
    is_enabled = TRUE,
    updated_at = now()
FROM cynexus_appl_ai.ai_providers provider
WHERE binding.capability = 'media_transcription_enrich'
  AND provider.code = 'grok'
  AND provider.deleted_at IS NULL;

COMMIT;
