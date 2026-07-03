CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.mkt_industry_application_target_catalog (
    id BIGINT NOT NULL PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    symbol VARCHAR(32) NOT NULL UNIQUE,
    code6 VARCHAR(16) NOT NULL,
    exchange VARCHAR(16) NOT NULL,
    name VARCHAR(128) NOT NULL,
    source VARCHAR(128),
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_mkt_ind_app_target_catalog_type
    ON cynexus_appl_market.mkt_industry_application_target_catalog(target_type, name)
    WHERE deleted_at IS NULL;

INSERT INTO cynexus_appl_market.mkt_industry_application_target_catalog (
    id, target_type, symbol, code6, exchange, name, source, created_at, updated_at
)
VALUES
    (202607030001, 'index', 'sh000001', '000001', 'sh', '上证指数', 'migration:index-seed', now(), now()),
    (202607030002, 'index', 'sh000016', '000016', 'sh', '上证50', 'migration:index-seed', now(), now()),
    (202607030003, 'index', 'sh000300', '000300', 'sh', '沪深300', 'migration:index-seed', now(), now()),
    (202607030004, 'index', 'sh000688', '000688', 'sh', '科创50', 'migration:index-seed', now(), now()),
    (202607030005, 'index', 'sh000852', '000852', 'sh', '中证1000', 'migration:index-seed', now(), now()),
    (202607030006, 'index', 'sh000905', '000905', 'sh', '中证500', 'migration:index-seed', now(), now()),
    (202607030007, 'index', 'sh000932', '000932', 'sh', '中证2000', 'migration:index-seed', now(), now()),
    (202607030008, 'index', 'sz399001', '399001', 'sz', '深证成指', 'migration:index-seed', now(), now()),
    (202607030009, 'index', 'sz399006', '399006', 'sz', '创业板指', 'migration:index-seed', now(), now())
ON CONFLICT (symbol) DO UPDATE SET
    target_type = EXCLUDED.target_type,
    code6 = EXCLUDED.code6,
    exchange = EXCLUDED.exchange,
    name = EXCLUDED.name,
    source = EXCLUDED.source,
    updated_at = now(),
    deleted_at = NULL;
