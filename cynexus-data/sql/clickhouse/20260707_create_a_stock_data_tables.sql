CREATE DATABASE IF NOT EXISTS cynexus;

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_stock_fund_flow_minute
(
    source LowCardinality(String),
    code LowCardinality(String),
    trade_date Date,
    trade_time DateTime64(3),
    main_net Nullable(Decimal(24, 4)),
    small_net Nullable(Decimal(24, 4)),
    mid_net Nullable(Decimal(24, 4)),
    large_net Nullable(Decimal(24, 4)),
    super_net Nullable(Decimal(24, 4)),
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (source, code, trade_time);

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_stock_transaction
(
    source LowCardinality(String),
    code LowCardinality(String),
    trade_date Date,
    trade_time String,
    price Nullable(Decimal(18, 4)),
    volume Nullable(Int64),
    amount Nullable(Decimal(24, 4)),
    buy_sell Nullable(String),
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (source, code, trade_date, trade_time);

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_stock_order_book_snapshot
(
    source LowCardinality(String),
    code LowCardinality(String),
    quote_time DateTime64(3),
    trade_date Date,
    price Nullable(Decimal(18, 4)),
    open Nullable(Decimal(18, 4)),
    high Nullable(Decimal(18, 4)),
    low Nullable(Decimal(18, 4)),
    pre_close Nullable(Decimal(18, 4)),
    volume Nullable(Int64),
    amount Nullable(Decimal(24, 4)),
    bid_prices String,
    ask_prices String,
    bid_volumes String,
    ask_volumes String,
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (source, code, quote_time);

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_baidu_kline_ma
(
    source LowCardinality(String),
    code LowCardinality(String),
    trade_date Date,
    open Nullable(Decimal(18, 4)),
    high Nullable(Decimal(18, 4)),
    low Nullable(Decimal(18, 4)),
    close Nullable(Decimal(18, 4)),
    volume Nullable(Int64),
    amount Nullable(Decimal(24, 4)),
    ma5 Nullable(Decimal(18, 4)),
    ma10 Nullable(Decimal(18, 4)),
    ma20 Nullable(Decimal(18, 4)),
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYear(trade_date)
ORDER BY (source, code, trade_date);

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_northbound_minute
(
    source LowCardinality(String),
    trade_date Date,
    minute_time String,
    hgt_yi Nullable(Decimal(18, 4)),
    sgt_yi Nullable(Decimal(18, 4)),
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (source, trade_date, minute_time);

CREATE TABLE IF NOT EXISTS cynexus.ch_ods_option_quote_snapshot
(
    source LowCardinality(String),
    option_code LowCardinality(String),
    quote_time DateTime64(3),
    trade_date Date,
    name String,
    last Nullable(Decimal(18, 4)),
    bid Nullable(Decimal(18, 4)),
    ask Nullable(Decimal(18, 4)),
    strike Nullable(Decimal(18, 4)),
    volume Nullable(Int64),
    amount Nullable(Decimal(24, 4)),
    open_interest Nullable(Int64),
    delta Nullable(Decimal(18, 8)),
    gamma Nullable(Decimal(18, 8)),
    theta Nullable(Decimal(18, 8)),
    vega Nullable(Decimal(18, 8)),
    iv Nullable(Decimal(18, 8)),
    raw_payload String,
    ingested_at DateTime64(3)
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (source, option_code, quote_time);
