# Findings

## External eltdx 1.2.0

- Latest PyPI release inspected: `1.2.0`, released 2026-07-19.
- Data executor currently imports `eltdx 1.2.0`; `cynexus-data/pyproject.toml` still declares `eltdx>=1.0.2`.
- Useful capabilities: `helpers.shortline_indicators()` (21 fields), `quotes.refresh()`/`poll_push()`, `quotes.get_depth()`, `trades.today()`/`history()`, `minutes.history()`, `limits.scan_special()`, and 09:25 auction snapshot.
- `history_trade_day` is per-symbol and suitable for candidate stocks, not unrestricted all-market polling.
- `shortline_indicators` includes `limit_status`, `seal_to_float_ratio`, `seal_prev_ratio`, and `ladder_level`, but is based on TDX statistics plus current snapshots and must be date-aligned.
- Research-Only license needs to remain visible in operational documentation and limits production/commercial use.

## Existing data layer

- `cynexus-data` already has `eltdx` adapters under `src/cynexus_data/sources/tdx/`, ClickHouse 1m K-line storage, PostgreSQL repositories, a trigger API, and scheduler-owned jobs.
- Existing `sync_kline_minute` reads local TDX `.lc1` files; this is separate from the new live indicator workflow.
- Existing market data tables and migrations use `cynexus_appl_market`, bigint IDs, soft-delete columns, JSONB `extra/raw_payload`, source labels, and idempotent unique indexes.

## Existing scheduler observations

- Market pulse currently refreshes every 10 minutes but does not provide the new per-stock indicator workflow.
- THS and TDX sector jobs are daily/after-close; they are not suitable as the live indicator collector.

## UI conventions

- Product uses `WorkspaceShell`, semantic Tailwind tokens, shadcn/Radix primitives, dense tables, and market red-up/green-down semantics.
- New route must update `src/router/index.tsx`, `src/config/sidebar.ts`, breadcrumb vocabulary, and feature-specific services/components.

## Implementation verification

- `sec_stock_basic.limit_up_threshold` stores ratios (`0.10`, `0.20`, `0.30`), while quote `change_pct` is percentage points; the indicator domain now converts thresholds before classification.
- Release smoke job `market_indicator_refresh` completed successfully and wrote live eltdx snapshots to PostgreSQL.
- The API and UI route both returned HTTP 200 from the Release stack.
- The collector is periodic quote monitoring, not an unlimited raw-tick archive; exact tick history remains a separate data-source concern.

## Extension design

- Existing `sec_stock_basic.industry` and `sec_stock_sector` joined to `sec_sector.category_raw = 0` provide the industry mapping needed by the indicator workflow.
- Market counts must be calculated from the complete latest-per-code snapshot set, not from the UI-filtered/paginated rows.
- Industry rows will persist per capture with limit-up/down counts, rapid-event counts, first limit-up stock/time, and first event stock/time so the page can explain both breadth and intra-industry sequencing.
