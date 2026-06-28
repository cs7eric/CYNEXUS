# TDX Industry Quotes Data Contract

## PostgreSQL Tables

`cynexus_data.tdx_industry_quote_batch`

- One capture batch per job run.
- Stores `batch_id`, `snapshot_date`, `source`, `row_count`, and capture timestamps.

`cynexus_data.tdx_industry_quote_snapshot`

- One active row per `snapshot_date, code6, source`.
- Stores TDX 56 industry quote fields from eltdx:
  - `code6`, `name`, `full_code`, `exchange`
  - `last_price`, `pre_close_price`, `open_price`, `high_price`, `low_price`
  - `change`, `change_pct`
  - `amount`, `total_hand`, `current_hand`

This is a lightweight Java/UI serving snapshot. Raw high-frequency market facts still belong in ClickHouse.

## Source Boundary

`sources/tdx/industry_quotes.py` only resolves TDX industry full codes and normalizes quote snapshots. It does not write storage or manage jobs.
