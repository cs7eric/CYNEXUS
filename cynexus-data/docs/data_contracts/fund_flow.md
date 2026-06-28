# Fund Flow Data Contract

## PostgreSQL Tables

`cynexus_appl_market.sector_fund_flow_capture_batches`

- One active capture batch per `scope + trade_date`.
- Old batches are soft-deleted before a retry writes the replacement batch.
- Stores source metadata: `source`, `source_type`, `total_pages`, `page_row_counts`, `fetched_at`, and `extra`.

`cynexus_appl_market.sector_fund_flow_daily_snapshots`

- One active row per `scope + trade_date + sector_name`.
- Stores normalized THS industry fund-flow fields:
  - `sector_code`, `sector_name`, `rank`
  - `change_pct`, `inflow`, `outflow`, `net`
  - `company_count`, `leader_stock`, `leader_change`, `leader_price`
  - `source`, `source_type`, `captured_at`, `extra`

## Scope

- `industry`: THS industry fund-flow snapshot.
- `concept`: reserved for future concept sector flow; the current production pipeline only implements `industry`.

## Source Types

- `crawler`: live THS `data.10jqka.com.cn` scrape with `hexin-v`.
- `json_import`: local historical JSON import from the old project/reference archive.
