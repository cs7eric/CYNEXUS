# Fund Flow Pipeline

## Jobs

- `sync_fund_flow`: sector-level THS snapshots.
- `sync_stock_fund_flow`: stock-level Tencent qt snapshots.

## Sector Production Path

`sync_fund_flow` migrates legacy THS industry fund-flow production logic:

1. Read from local history JSON via `history_path`, or fetch live THS industry fund-flow pages.
2. Normalize source rows in `domain.market.fund_flow`.
3. Replace the active PostgreSQL snapshot for `scope + trade_date`.
4. Keep old rows soft-deleted for retry traceability.

## Parameters

- `trade_date`: target trade date, defaults to the current local date.
- `scope`: `industry` by default.
- `provider` / `providers`: only `ths` is implemented for sector flow.
- `history_path`: file or directory of `YYYY-MM-DD.json` payloads. If a directory contains an exact trade-date file, that file is used; otherwise all JSON files are imported.
- `create_table`: applies `sql/postgres/20260628_create_sector_fund_flow_tables.sql`.
- `max_pages`: live THS page cap for smoke tests.
- `page_sleep`: live THS page delay.

## Stock Production Path

`sync_stock_fund_flow` migrates the scoped data-production part of the legacy Tencent `qt.gtimg.cn` stock fund-flow proxy. It does not migrate the old Flask API or disk JSON cache.

1. Read from local history JSON via `history_path`, or fetch live Tencent qt quote + `s_pk` rows for explicit `codes`.
2. If `codes` is omitted in non-dry-run mode, read active symbols from `sec_stock_basic`.
3. Normalize source rows in `domain.market.fund_flow`.
4. Replace the active PostgreSQL stock snapshot for `trade_date + source`.
5. Keep batch metadata with requested codes and source lineage.

Tencent qt provides active buy/sell totals (`outer_disc_lots` / `inside_dish_lots`) and `s_pk` disk ratios. It does not provide a true main/large/mid/small order breakdown; that remains a separate provider-specific migration if a reliable source is added.

Stock parameters:

- `trade_date`: target trade date, defaults to the current local date.
- `provider` / `providers`: only `tencent` is implemented for stock flow.
- `codes` / `symbols`: comma-separated symbols such as `600000,000001` or market-prefixed `sh600000`.
- `history_path`: file or directory of `YYYY-MM-DD.json` payloads.
- `create_table`: applies `sql/postgres/20260628_create_stock_fund_flow_tables.sql`.
- `batch_size`: Tencent batch request size, defaults to `120`.

Dry run:

```powershell
python -m cynexus_data.jobs.sync_stock_fund_flow --dry-run --trade-date 2026-06-26 --codes 600000,000001
```

## Boundary

- Source modules parse/fetch only; they do not write storage.
- Domain modules normalize and rank only; they do not read env, network, or databases.
- The pipeline owns orchestration and may read local JSON archives.
- High-frequency raw transaction/order-book flows still belong in ClickHouse; this pipeline stores lightweight per-stock snapshots in PostgreSQL for UI/business consumption.
- Concept flow remains a separate provider-specific migration.
