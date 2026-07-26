ALTER TABLE cynexus_appl_ai.ai_agent_conversation
    ADD COLUMN IF NOT EXISTS provider_response_id varchar(255),
    ADD COLUMN IF NOT EXISTS provider_state_provider_code varchar(128),
    ADD COLUMN IF NOT EXISTS provider_state_model_code varchar(128),
    ADD COLUMN IF NOT EXISTS provider_state_signature varchar(255),
    ADD COLUMN IF NOT EXISTS provider_state_updated_at timestamptz;

ALTER TABLE cynexus_appl_ai.ai_agent_run_event
    ADD COLUMN IF NOT EXISTS details_json jsonb;

COMMENT ON COLUMN cynexus_appl_ai.ai_agent_conversation.provider_response_id IS
    'Last stored Responses API response ID used for conversation continuation.';
COMMENT ON COLUMN cynexus_appl_ai.ai_agent_conversation.provider_state_signature IS
    'Hashed provider/model/prompt/tool state signature required before response ID reuse.';
COMMENT ON COLUMN cynexus_appl_ai.ai_agent_run_event.details_json IS
    'Structured, redacted event diagnostics such as per-turn prompt-cache telemetry.';

UPDATE cynexus_appl_ai.ai_usage_bindings
SET params = COALESCE(params, '{}'::jsonb) || jsonb_build_object(
        'responses_state_mode', 'previous_response_id',
        'disable_response_storage', false,
        'prompt_cache_retention', '24h'
    ),
    updated_at = now()
WHERE capability = 'autonomous_agent';
