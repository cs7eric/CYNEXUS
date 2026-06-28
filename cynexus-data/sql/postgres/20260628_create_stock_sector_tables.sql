CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.sec_sector
(
    sector_id text PRIMARY KEY,
    category_raw integer NOT NULL,
    category_label text NOT NULL,
    name text NOT NULL,
    topic_id text,
    source text NOT NULL DEFAULT 'eltdx',
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sec_sector_category_name
    ON cynexus_data.sec_sector (category_raw, name);

CREATE TABLE IF NOT EXISTS cynexus_data.sec_stock_sector
(
    code text NOT NULL,
    sector_id text NOT NULL REFERENCES cynexus_data.sec_sector(sector_id) ON DELETE CASCADE,
    relation_level numeric(12,6),
    reason text,
    source text NOT NULL DEFAULT 'eltdx',
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (code, sector_id)
);

CREATE INDEX IF NOT EXISTS idx_sec_stock_sector_sector
    ON cynexus_data.sec_stock_sector (sector_id, code);
