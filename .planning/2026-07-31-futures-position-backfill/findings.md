# Findings

## Initial Context

- User requests backfill of all available futures history, targeting four years; shorter ranges are acceptable where sources do not provide more.
- Current known page/API history window is 120 valid trading days, but this task concerns data storage/backfill rather than only the page window.
- CFFEX, SHFE, CZCE, GFEX, and INE use AkShare; DCE uses Sina fallback because the official endpoint returns HTTP 412.

## Runtime Findings

- The shared data executor is healthy at `http://127.0.0.1:18083`.
- `sync_futures_position` accepts one `trade_date` per run. The existing pipeline safely leaves the selected snapshot unchanged when a source returns no rows.
- DCE Sina history is materially slower: a probe for `2025-10-21` took about 32 seconds and returned 6 contracts / 360 rank rows.
- AkShare CFFEX probe for `2025-10-21` took about 1.5 seconds and returned 20 instruments / 1,200 rank rows.
- AkShare's `tool_trade_date_hist_sina` supplies 969 dates from `2022-07-30` through `2026-07-30`; using those dates avoids weekend/holiday requests.
- Backfill can run by exchange with distinct `lock_scope` values, allowing bounded concurrency without changing the application code or release image.
