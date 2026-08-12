CREATE TABLE IF NOT EXISTS ch_ods_trade_review_timeshare_1m
(
    trade_date Date,
    target_type LowCardinality(String),
    market LowCardinality(String),
    symbol String,
    tdx_code String,
    slot UInt16,
    trade_time DateTime('Asia/Shanghai'),
    price Decimal64(4),
    avg_price Nullable(Decimal64(4)),
    volume_lots UInt64,
    turnover Nullable(Decimal64(2)),
    price_source LowCardinality(String),
    amount_source LowCardinality(String),
    source_version LowCardinality(String),
    fetched_at DateTime64(3, 'Asia/Shanghai'),
    ingested_at DateTime64(3, 'Asia/Shanghai')
)
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (trade_date, target_type, market, symbol, slot, price_source);
