CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.daily_raw
(
    code LowCardinality(String),
    trade_date Date,
    open Decimal(12, 4),
    high Decimal(12, 4),
    low Decimal(12, 4),
    close Decimal(12, 4),
    volume Int64,
    amount Decimal(18, 4),
    unit_scale Int8 DEFAULT 1,
    source LowCardinality(String),
    ingested_at DateTime64(3)
)
ENGINE = MergeTree
PARTITION BY toYear(trade_date)
ORDER BY (code, trade_date);

CREATE TABLE IF NOT EXISTS cynexus.daily_qfq
(
    code LowCardinality(String),
    trade_date Date,
    open Decimal(12, 4),
    high Decimal(12, 4),
    low Decimal(12, 4),
    close Decimal(12, 4),
    volume Int64,
    amount Decimal(18, 4),
    adj_factor Decimal(14, 8),
    ingested_at DateTime64(3)
)
ENGINE = MergeTree
PARTITION BY toYear(trade_date)
ORDER BY (code, trade_date);

CREATE TABLE IF NOT EXISTS cynexus.daily_hfq
(
    code LowCardinality(String),
    trade_date Date,
    open Decimal(12, 4),
    high Decimal(12, 4),
    low Decimal(12, 4),
    close Decimal(12, 4),
    volume Int64,
    amount Decimal(18, 4),
    adj_factor Decimal(14, 8),
    ingested_at DateTime64(3)
)
ENGINE = MergeTree
PARTITION BY toYear(trade_date)
ORDER BY (code, trade_date);

CREATE TABLE IF NOT EXISTS cynexus.adj_factors
(
    code LowCardinality(String),
    trade_date Date,
    factor Decimal(14, 8),
    updated_at DateTime64(3)
)
ENGINE = MergeTree
PARTITION BY toYear(trade_date)
ORDER BY (code, trade_date);

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
ORDER BY (code, trade_date);

CREATE TABLE IF NOT EXISTS cynexus.ingest_state
(
    code LowCardinality(String),
    day_file_path String,
    processed_bytes Int64 DEFAULT 0,
    last_trade_date Nullable(Date),
    last_unit_scale Nullable(Int8),
    last_run_at Nullable(DateTime64(3)),
    last_status Nullable(String)
)
ENGINE = MergeTree
ORDER BY code;

CREATE TABLE IF NOT EXISTS cynexus.intraday_bars
(
    code LowCardinality(String),
    period LowCardinality(String),
    trade_time DateTime64(3),
    trade_date Date,
    open Decimal(12, 4),
    high Decimal(12, 4),
    low Decimal(12, 4),
    close Decimal(12, 4),
    volume Int64,
    amount Decimal(18, 4),
    source LowCardinality(String)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(trade_date)
ORDER BY (code, period, trade_time);

CREATE TABLE IF NOT EXISTS cynexus.quotes
(
    code LowCardinality(String),
    quote_time DateTime64(3),
    price Decimal(12, 4),
    open Nullable(Decimal(12, 4)),
    high Nullable(Decimal(12, 4)),
    low Nullable(Decimal(12, 4)),
    pre_close Nullable(Decimal(12, 4)),
    volume Nullable(Int64),
    amount Nullable(Decimal(18, 4)),
    bid_prices Nullable(String),
    ask_prices Nullable(String),
    bid_vols Nullable(String),
    ask_vols Nullable(String),
    source LowCardinality(String)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(toDate(quote_time))
ORDER BY (code, quote_time);

CREATE TABLE IF NOT EXISTS cynexus.schema_meta
(
    key String,
    value String,
    updated_at DateTime64(3)
)
ENGINE = MergeTree
ORDER BY key;
