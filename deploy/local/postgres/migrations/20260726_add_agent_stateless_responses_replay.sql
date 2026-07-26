ALTER TABLE cynexus_appl_ai.ai_agent_message
    ADD COLUMN IF NOT EXISTS runtime_context text;

ALTER TABLE cynexus_appl_ai.ai_agent_message
    ADD COLUMN IF NOT EXISTS provider_output jsonb;

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_message.runtime_context IS
    'Internal dynamic system input that immediately preceded this assistant response.';

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_message.provider_output IS
    'Opaque provider-native Responses output items used for lossless stateless replay.';

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET params = (COALESCE(binding.params, '{}'::jsonb) - 'responses_state_mode')
                || jsonb_build_object(
                    'wire_api', 'responses',
                    'responses_transport', 'http',
                    'disable_response_storage', true,
                    'prompt_cache_retention', '24h'),
       updated_at = now()
  FROM cynexus_appl_ai.ai_providers provider
 WHERE binding.provider_id = provider.id
   AND binding.capability = 'autonomous_agent'
   AND binding.deleted_at IS NULL
   AND provider.deleted_at IS NULL
   AND provider.code = 'xhhai';

UPDATE cynexus_appl_ai.ai_agent_conversation
   SET provider_response_id = NULL,
       provider_state_provider_code = NULL,
       provider_state_model_code = NULL,
       provider_state_signature = NULL,
       provider_state_updated_at = NULL,
       updated_at = now()
 WHERE provider_state_provider_code = 'xhhai'
   AND deleted_at IS NULL;
