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
