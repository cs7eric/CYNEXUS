CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_etf_mapping
(
    id bigint PRIMARY KEY,
    etf_code varchar(6) NOT NULL,
    mapping_group varchar(32) NOT NULL,
    mapping_code varchar(64) NOT NULL,
    mapping_name text NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    confidence numeric(5, 4),
    source text NOT NULL,
    effective_from date NOT NULL DEFAULT CURRENT_DATE,
    effective_to date,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_etf_mapping_group CHECK (mapping_group IN ('universe', 'asset_class', 'region', 'index_scope', 'style', 'theme')),
    CONSTRAINT ck_market_etf_mapping_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_etf_mapping_key_alive
    ON cynexus_appl_market.market_etf_mapping (etf_code, mapping_group, mapping_code, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_mapping_group_code
    ON cynexus_appl_market.market_etf_mapping (mapping_group, mapping_code, etf_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_etf_mapping_etf_code
    ON cynexus_appl_market.market_etf_mapping (etf_code, mapping_group, is_primary)
    WHERE deleted_at IS NULL;
