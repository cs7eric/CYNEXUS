\set ON_ERROR_STOP on

BEGIN;

-- Keep the configured primary route, but after a quota, timeout, or provider
-- failure try up to four independently configured providers. The cap prevents
-- a single upload from fanning out across every configured account.
UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET params = COALESCE(binding.params, '{}'::jsonb)
                || jsonb_build_object(
                    'request_timeout_seconds', 100,
                    'fallback_provider_codes', COALESCE((
                        SELECT jsonb_agg(candidate.code ORDER BY candidate.priority, candidate.id)
                          FROM (
                              SELECT provider.code,
                                     provider.id,
                                     CASE provider.code
                                         WHEN 'openai' THEN 1
                                         WHEN 'custom-yancc' THEN 2
                                         WHEN 'xhhai' THEN 3
                                         WHEN 'xhhai-grok' THEN 4
                                         WHEN 'grok' THEN 5
                                         ELSE 9
                                     END AS priority
                                FROM cynexus_appl_ai.ai_providers provider
                               WHERE provider.is_enabled
                                 AND provider.deleted_at IS NULL
                                 AND provider.id <> binding.provider_id
                               ORDER BY priority, provider.id
                               LIMIT 4
                          ) candidate
                    ), '[]'::jsonb)
                ),
       updated_at = now()
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND binding.is_enabled
   AND binding.deleted_at IS NULL;

COMMIT;
