# Progress

## 2026-08-11

- Read data and deployment instructions.
- Confirmed CodeGraph index is available; it reports one pending file change from the existing worktree state.
- Inspected the market indicator PostgreSQL DDL/repository, current scheduler session, and TDX indicator tests.
- Decided on one structured PostgreSQL snapshot table with explicit five-level fields and sealed-amount columns.
- Added the durable DDL, Release migration, and repository `create_table()` wiring.
- Added pure five-level normalization,封板额 calculation, state-transition mapping, and PostgreSQL upsert helpers with focused tests.
- Focused tests passed: `7 passed`.
- Applied `20260811_create_market_indicator_limit_board_snapshots.sql` to the shared PostgreSQL; the table exists with 43 columns and the expected primary/unique/query indexes.
- Maven market build succeeded during Release verification. Docker image build failed while fetching `eclipse-temurin:21-jre-jammy` from Docker Hub, so the existing market container was left running.
- Confirmed the new table had zero rows, then reduced persistence to best bid/ask only and added an idempotent drop-column migration.
- Applied the reduction migration successfully; the live table now has 27 columns, no level-2 through level-5 columns, and zero rows.
- Re-ran focused tests after preserving zero-volume quotes: `7 passed`.
- User approved implementation of the two-session Redis-backed limit-board monitor and PostgreSQL session summaries.
- Added `market_indicator_limit_board_session_summaries` DDL, migration, create-table wiring, and applied the migration; the table is empty and ready for the finalize writer.
- Confirmed the existing session wrapper is compatibility code around full-market refresh; the new monitor will use independent capture/finalize jobs and a compact Redis List session format.
- Added compressed Redis session cache with `samples`, `seen`, `directions`, and `meta` keys, with a 12-hour TTL and post-commit deletion.
- Added pure session aggregation for sealed duration, open/reseal transitions, coverage gaps, amount statistics, slope, trend direction, and bounded trend points.
- Added `market_indicator_limit_board_capture` and `market_indicator_limit_board_finalize` jobs, registry/config entries, and idempotent scheduler migration. Capture runs every 5 seconds in both trading windows; finalize retries in post-session windows.
- Added focused coverage for sample transitions, session inference, Redis round trips, PG parameter shaping, finalize success deletion, and finalize failure retention. Focused tests pass: `14 passed`.
- Applied `20260811_schedule_market_indicator_limit_board.sql`; both scheduler jobs are enabled with tick intervals 5 seconds and 60 seconds.
- Rebuilt and replaced `cynexus-data-job-executor`; the new jobs are registered in the healthy container and capture dry-run passed.
- Release market Maven build and migrations passed, but the market Docker image build was blocked by Docker Hub connectivity while fetching `eclipse-temurin:21-jre-jammy`; the existing market container remains running.
