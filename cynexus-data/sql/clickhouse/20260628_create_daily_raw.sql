CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.daily_raw
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
    unit_scale UInt16 DEFAULT 1,
    source LowCardinality(String) DEFAULT 'tdx_day',
    source_file String,
    source_file_mtime DateTime64(3),
    ingested_at DateTime64(3),
    row_hash UInt64 MATERIALIZED cityHash64(market, symbol, trade_date, open, high, low, close, amount, volume, unit_scale, source)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYear(trade_date)
ORDER BY (market, symbol, trade_date, source)
SETTINGS index_granularity = 8192;
