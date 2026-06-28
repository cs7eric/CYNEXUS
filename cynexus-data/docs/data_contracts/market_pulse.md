# Market Pulse Data Contract

## PostgreSQL Tables

`cynexus_data.market_pulse_capture_batch`

- One active batch per trade date.
- Source is normally `sector_fund_flow_daily_snapshot`.
- Old batches are soft-deleted on replacement.

`cynexus_data.market_pulse_sector_daily_snapshot`

- One active row per `trade_date + sector_name`.
- Stores UI-ready industry pulse fields:
  - `rank_by_change`, `change_pct`
  - `inflow`, `outflow`, `main_net`
  - `stock_count`, `leading_stock`, `leading_change_pct`, `leading_price`

## Source

The first production path derives market pulse rows from `sector_fund_flow_daily_snapshot`, which is populated by the THS industry fund-flow migration.
