CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_information_content (
    id bigint PRIMARY KEY,
    item_kind text NOT NULL,
    source text NOT NULL,
    code text NOT NULL,
    item_id text NOT NULL,
    title text,
    source_url text,
    resolved_url text,
    content_type text,
    fetch_status text NOT NULL,
    content_text text,
    content_hash text,
    error_message text,
    extracted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_information_content_item
    ON cynexus_appl_market.a_stock_information_content (item_kind, source, code, item_id);
CREATE INDEX IF NOT EXISTS idx_a_stock_information_content_hash
    ON cynexus_appl_market.a_stock_information_content (content_hash)
    WHERE content_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_information_analysis (
    id bigint PRIMARY KEY,
    content_id bigint NOT NULL REFERENCES cynexus_appl_market.a_stock_information_content(id),
    content_hash text NOT NULL,
    analysis_schema_version text NOT NULL,
    prompt_version text NOT NULL,
    model_code text NOT NULL DEFAULT '',
    status text NOT NULL,
    result_json text,
    error_message text,
    analyzed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_information_analysis_cache
    ON cynexus_appl_market.a_stock_information_analysis (content_id, content_hash, analysis_schema_version, prompt_version, model_code);
