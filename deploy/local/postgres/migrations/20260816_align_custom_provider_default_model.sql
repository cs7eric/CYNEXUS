\set ON_ERROR_STOP on

BEGIN;

-- The configured custom account rejects gpt-5.4-mini. gpt-5.4 is present in
-- the account model catalog and is the verified route used by Screener AI.
UPDATE cynexus_appl_ai.ai_providers
SET default_model = 'gpt-5.4',
    updated_at = now()
WHERE code = 'custom'
  AND default_model = 'gpt-5.4-mini'
  AND is_enabled
  AND deleted_at IS NULL;

COMMIT;
