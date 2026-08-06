# Findings

- The database contained only selected futures-position batches for 2026-08-04; 2026-08-03 was absent.
- A manual `sync_futures_position` run with `trade_date=20260803` succeeded for all six exchanges and wrote 18,290 rows.
- The deployed latest-source run resolved 2026-08-04 and therefore skipped the earlier missing 2026-08-03 date.
- The source adapters can return data for an explicitly requested historical date, so the missing date was a recovery gap rather than an unavailable source date.

