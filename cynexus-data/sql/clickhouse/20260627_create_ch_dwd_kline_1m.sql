CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.ch_dwd_kline_1m
(
    market LowCardinality(String),
    symbol String,
    tdx_code String,
    period UInt16 DEFAULT 1,
    trade_time DateTime,
    trade_date Date,
    open Float64,
    high Float64,
    low Float64,
    close Float64,
    amount Float64,
    volume Int64,
    adj_factor Nullable(Float64),
    source LowCardinality(String) DEFAULT 'tdx_local',
    source_file String,
    source_file_mtime DateTime64(3),
    ingested_at DateTime64(3),
    row_hash UInt64 MATERIALIZED cityHash64(market, symbol, period, trade_time, open, high, low, close, amount, volume, ifNull(adj_factor, 0), source)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYear(trade_date)
ORDER BY (market, symbol, period, trade_time, source)
SETTINGS index_granularity = 8192;
