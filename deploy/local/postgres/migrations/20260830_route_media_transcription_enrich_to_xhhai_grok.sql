\set ON_ERROR_STOP on
BEGIN;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
SET provider_id = provider.id,
    model_override = 'grok-4.6',
    is_enabled = TRUE,
    updated_at = now()
FROM cynexus_appl_ai.ai_providers provider
WHERE binding.capability IN ('media_transcription_enrich', 'text_polish', 'text_summary', 'post_metadata', 'mp4_qa')
  AND provider.code = 'xhhai-grok'
  AND provider.deleted_at IS NULL;

COMMIT;
