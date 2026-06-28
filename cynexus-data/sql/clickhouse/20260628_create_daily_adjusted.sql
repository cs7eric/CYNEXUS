CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.daily_qfq
(
    market LowCardinality(String),
    symbol String,
    code String,
    tdx_code String,
    trade_date Date,
    open Float64,
    high Float64,
    low Float64,
    close Float64,
    amount Float64,
    volume Int64,
    adj_factor Float64 DEFAULT 1.0,
    adjust LowCardinality(String) DEFAULT 'qfq',
    source LowCardinality(String) DEFAULT 'eltdx',
    ingested_at DateTime64(3),
    row_hash UInt64 MATERIALIZED cityHash64(market, symbol, trade_date, open, high, low, close, amount, volume, adj_factor, adjust, source)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYear(trade_date)
ORDER BY (market, symbol, trade_date, adjust, source)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS cynexus.daily_hfq
(
    market LowCardinality(String),
    symbol String,
    code String,
    tdx_code String,
    trade_date Date,
    open Float64,
    high Float64,
    low Float64,
    close Float64,
    amount Float64,
    volume Int64,
    adj_factor Float64 DEFAULT 1.0,
    adjust LowCardinality(String) DEFAULT 'hfq',
    source LowCardinality(String) DEFAULT 'eltdx',
    ingested_at DateTime64(3),
    row_hash UInt64 MATERIALIZED cityHash64(market, symbol, trade_date, open, high, low, close, amount, volume, adj_factor, adjust, source)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYear(trade_date)
ORDER BY (market, symbol, trade_date, adjust, source)
SETTINGS index_granularity = 8192;
