# Kline Data Contract

## ClickHouse Minute Fact

Primary table: `ch_dwd_kline_1m`.

The table stores 1m raw/minute facts and derived minute periods. `period=1` is source data. Aggregated bars use `period` values such as `5`, `15`, `30`, and `60`, with `source='aggregate_1m'`.

Identity:

- `market`
- `symbol`
- `period`
- `trade_time`
- `source`

OHLCV rules:

- `open`, `high`, `low`, and `close` must be positive.
- `high` must be greater than or equal to `open`, `low`, and `close`.
- `low` must be less than or equal to `open`, `high`, and `close`.
- `volume` and `amount` must be non-negative.

Aggregation rules:

- `open`: first 1m open by `trade_time`.
- `high`: max high.
- `low`: min low.
- `close`: last 1m close by `trade_time`.
- `volume`: sum volume.
- `amount`: sum amount.
- `trade_time`: `toStartOfInterval(trade_time, INTERVAL <period> minute)`.

## ClickHouse Daily Raw

Primary table: `daily_raw`.

Source rows are parsed from local TDX `.day` files. The table stores unadjusted daily OHLCV facts.

Identity:

- `market`
- `symbol`
- `trade_date`
- `source`

Fields:

- `market`: `SH`, `SZ`, or `BJ`.
- `symbol` / `code`: six-digit security code.
- `tdx_code`: TDX market-prefixed code such as `sz000001`.
- `unit_scale`: applied TDX price unit correction. Default is `1`.
- `source_file` and `source_file_mtime`: lineage back to the local `.day` file.

## ClickHouse Adjusted Daily

Primary tables:

- `daily_qfq`
- `daily_hfq`

These tables store eltdx-provided adjusted daily K-line facts.

Identity:

- `market`
- `symbol`
- `trade_date`
- `adjust`
- `source`

Fields are aligned with `daily_raw`, with additional:

- `adjust`: `qfq` or `hfq`.
- `adj_factor`: factor reported by the provider when available, otherwise `1.0`.

## ClickHouse Intraday Detail

Primary tables:

- `ch_ods_stock_auction_point`
- `ch_ods_stock_timeshare_1m`

These tables store eltdx-provided high-frequency intraday detail. They are ClickHouse facts, not PostgreSQL UI result tables.

Auction identity:

- `trade_date`
- `market`
- `symbol`
- `trade_time`
- `phase`
- `source`

Timeshare identity:

- `trade_date`
- `market`
- `symbol`
- `trade_time`
- `source`

Shared lineage fields:

- `tdx_code`: provider-specific market-prefixed code.
- `raw_payload`: compact JSON copy of the provider row used to build the normalized fact.
- `fetched_at` and `ingested_at`: capture time and ClickHouse version column.

`phase` is one of `opening`, `auction_0925`, `continuous`, or `closing`. Re-runs append newer versions through `ReplacingMergeTree(ingested_at)`.

## Reader Compatibility Payload

`storage/clickhouse/readers.py` exposes `read_daily_kline_bars` for legacy-compatible daily K-line reads.

Returned rows contain:

- `timestamp`: Beijing trading-session timestamp at 09:30 in epoch milliseconds.
- `trade_date`, `open`, `high`, `low`, `close`, `volume`, `amount`.
- `adj_factor` when reading `daily_qfq` or `daily_hfq`.

Helper reads:

- `get_latest_daily_trade_date`
- `has_adjusted_daily`
- `list_codes_with_adjusted_daily`
