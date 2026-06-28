# Market Pulse Data Contract

## PostgreSQL Tables

`cynexus_appl_market.market_pulse_capture_batches`

- One active batch per trade date.
- Source is normally `sector_fund_flow_daily_snapshots`.
- Old batches are soft-deleted on replacement.

`cynexus_appl_market.market_pulse_sector_daily_snapshots`

- One active row per `trade_date + sector_name`.
- Stores UI-ready industry pulse fields:
  - `rank_by_change`, `change_pct`
  - `inflow`, `outflow`, `main_net`
  - `stock_count`, `leading_stock`, `leading_change_pct`, `leading_price`

## Source

The first production path derives market pulse rows from `sector_fund_flow_daily_snapshots`, which is populated by the THS industry fund-flow migration.
