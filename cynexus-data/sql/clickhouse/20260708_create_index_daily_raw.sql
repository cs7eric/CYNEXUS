CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.index_daily_raw
(
    code LowCardinality(String),
    trade_date Date,
    open Decimal(12, 4),
    high Decimal(12, 4),
    low Decimal(12, 4),
    close Decimal(12, 4),
    volume Int64,
    amount Decimal(18, 4),
    source LowCardinality(String),
    ingested_at DateTime64(3)
)
ENGINE = MergeTree
PARTITION BY toYear(trade_date)
ORDER BY (code, trade_date)
SETTINGS index_granularity = 8192;
