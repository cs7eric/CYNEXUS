\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_providers (
    id, code, name, provider_type, base_url, default_model, api_key_env,
    is_enabled, timeout_seconds, extra, models, remark
) VALUES (
    910001000000000006,
    'custom-yancc',
    'My Codex',
    'custom',
    'https://yancc.cloud/v1',
    'gpt-5.4',
    NULL,
    true,
    300,
    '{"wire_api":"responses","model_reasoning_effort":"low","disable_response_storage":true,"response_format":"json_object","request_timeout_seconds":300}'::jsonb,
    '["gpt-5.4"]'::jsonb,
    'Responses API provider. Secret is managed through AI Provider configuration.'
)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    provider_type = EXCLUDED.provider_type,
    base_url = EXCLUDED.base_url,
    default_model = EXCLUDED.default_model,
    api_key = cynexus_appl_ai.ai_providers.api_key,
    api_key_env = EXCLUDED.api_key_env,
    is_enabled = EXCLUDED.is_enabled,
    timeout_seconds = EXCLUDED.timeout_seconds,
    extra = EXCLUDED.extra,
    models = EXCLUDED.models,
    remark = EXCLUDED.remark,
    updated_at = now(),
    deleted_at = NULL;

COMMIT;
