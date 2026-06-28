CREATE TABLE IF NOT EXISTS ch_ods_stock_auction_point
(
    market LowCardinality(String),
    symbol String,
    code String,
    tdx_code String,
    trade_date Date,
    trade_time DateTime,
    time_label String,
    phase LowCardinality(String),
    price Nullable(Float64),
    matched_volume Nullable(Float64),
    matched_amount Nullable(Float64),
    unmatched_buy_volume Nullable(Float64),
    unmatched_sell_volume Nullable(Float64),
    source LowCardinality(String),
    raw_payload String,
    fetched_at DateTime,
    ingested_at DateTime
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (trade_date, market, symbol, trade_time, phase, source);

CREATE TABLE IF NOT EXISTS ch_ods_stock_timeshare_1m
(
    market LowCardinality(String),
    symbol String,
    code String,
    tdx_code String,
    trade_date Date,
    trade_time DateTime,
    time_label String,
    price Float64,
    avg_price Nullable(Float64),
    volume Float64,
    turnover Float64,
    turnover_rate Nullable(Float64),
    volume_ratio Nullable(Float64),
    source LowCardinality(String),
    raw_payload String,
    fetched_at DateTime,
    ingested_at DateTime
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (trade_date, market, symbol, trade_time, source);
