CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.sec_sector
(
    id bigint PRIMARY KEY,
    sector_id text NOT NULL,
    category_raw integer NOT NULL,
    category_label text NOT NULL,
    name text NOT NULL,
    topic_id text,
    source text NOT NULL DEFAULT 'eltdx',
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sec_sector_sector_id
    ON cynexus_appl_market.sec_sector (sector_id);

CREATE INDEX IF NOT EXISTS idx_sec_sector_category_name
    ON cynexus_appl_market.sec_sector (category_raw, name);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.sec_stock_sector
(
    id bigint PRIMARY KEY,
    code text NOT NULL,
    sector_id text NOT NULL REFERENCES cynexus_appl_market.sec_sector(sector_id) ON DELETE CASCADE,
    relation_level numeric(12,6),
    reason text,
    source text NOT NULL DEFAULT 'eltdx',
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sec_stock_sector_code_sector
    ON cynexus_appl_market.sec_stock_sector (code, sector_id);

CREATE INDEX IF NOT EXISTS idx_sec_stock_sector_sector
    ON cynexus_appl_market.sec_stock_sector (sector_id, code);
