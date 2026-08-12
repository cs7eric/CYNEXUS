# Findings

- `eltdx 1.3.0` returns `buy_levels` and `sell_levels`, each with five `price`/`volume` records.
- Existing `market_indicator_snapshots` keeps the provider payload in JSONB, but has no dedicated queue columns.
- Existing PostgreSQL migration placement is `deploy/local/postgres/migrations`; durable data DDL also lives under `cynexus-data/sql/postgres`.
- Existing market indicator repository uses explicit SQL with deterministic IDs and JSONB payloads.
- A 5-second capture of at most 100 codes is operationally reasonable for PostgreSQL, but unbounded raw JSONB growth should be avoided.
- The current worktree contains unrelated user changes; only new limit-board files should be changed in this task.
- `market_indicator_intraday_session` currently wraps the full market-indicator refresh and is not suitable for limit-board-only sampling because it stages large full-market JSON bundles.
- `fetch_indicator_snapshots` can normalize best bid/ask from eltdx quotes, but candidate discovery must be separated from five-second tracking to avoid querying the full historical pool every cycle.
- The new summary table is `market_indicator_limit_board_session_summaries`, unique by `trade_date`, `trade_session`, and `code`; its `trend_features` JSONB is reserved for derived features, not raw source samples.
- Shared Redis currently uses `appendonly yes`; session data needs a bounded TTL and a single packed append log per trading session rather than one JSON key per sample.
- The implementation will use one compressed Redis List entry per five-second batch, plus a session `seen` set and direction hash; this preserves opened/resealed transitions without creating one key per stock or sample.
- Capture and finalize are separate jobs: capture infers the active Shanghai session and never writes PostgreSQL; finalize reads one completed session, writes `(trade_date, trade_session, code)` summaries, then deletes the session keys only after commit.
