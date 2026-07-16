ALTER TABLE cynexus_appl_ai.ai_providers
    ADD COLUMN IF NOT EXISTS input_price_per_million numeric(18, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS output_price_per_million numeric(18, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pricing_currency varchar(8) NOT NULL DEFAULT 'USD';

UPDATE cynexus_appl_ai.ai_providers
SET pricing_currency = 'USD'
WHERE pricing_currency IS NULL OR btrim(pricing_currency) = '';

COMMENT ON COLUMN cynexus_appl_ai.ai_providers.input_price_per_million IS 'Input token price per one million tokens.';
COMMENT ON COLUMN cynexus_appl_ai.ai_providers.output_price_per_million IS 'Output token price per one million tokens.';
COMMENT ON COLUMN cynexus_appl_ai.ai_providers.pricing_currency IS 'Currency used for token pricing, normally USD.';
