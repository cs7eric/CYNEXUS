\set ON_ERROR_STOP on

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET params = COALESCE(binding.params, '{}'::jsonb)
                || '{"responses_transport":"http","responses_http_stream_mode":"buffered","disable_response_storage":true}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_providers provider
 WHERE binding.capability = 'autonomous_agent'
   AND binding.provider_id = provider.id
   AND provider.code = 'xhhai'
   AND binding.deleted_at IS NULL
   AND provider.deleted_at IS NULL;
