# Findings

- Existing page: `cynexus-ui/src/views/market/market-indicator/index.tsx` with private components under the same page folder.
- Existing route: `/market/indicator`; sidebar and global command palette point to it.
- New persisted data: `cynexus_appl_market.market_indicator_limit_board_session_summaries`, keyed by `(trade_date, trade_session, code)`, containing seal amount aggregates, duration, open/reseal counts, coverage, gap count, and trend data.
- No Java query endpoint currently exposes the new session summary table. UI must use a bounded Java market API rather than direct database access.
- Existing worktree contains extensive unrelated user changes; only task files will be touched.

