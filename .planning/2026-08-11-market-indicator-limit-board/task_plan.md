# Task Plan: Limit Board Intraday Monitor

## Goal

Implement the limit-up/limit-down intraday monitor: discover the active board pool, collect best bid/ask every five seconds into morning/afternoon Redis sessions, finalize derived time-trend features at each session close, persist one PostgreSQL summary per trade date/session/stock, then clean the Redis session after a successful transaction.

## Phases

- [completed] Confirm the existing market indicator, migration, and repository boundaries.
- [completed] Add the PostgreSQL schema and durable data contract for five-level limit-board snapshots.
- [completed] Add domain normalization and repository persistence helpers with focused tests.
- [completed] Verify migration wiring, tests, status, and deployment prerequisites.
- [completed] Reduce persisted order-book fields to best bid/ask only after confirming the new table is empty.
- [completed] Implement compact Redis session storage and pure session aggregation.
- [completed] Add PostgreSQL summary persistence, collection/finalization jobs, and scheduler wiring.
- [completed] Run focused tests, dry runs, migration checks, and runtime verification.

## Decisions

- PostgreSQL is the authoritative store for this first version; ClickHouse is not required.
- Store explicit queue fields instead of relying only on raw JSONB.
- Store only best bid/ask in PostgreSQL; the provider may still return five levels transiently.
- Keep the schema extensible for 5-second snapshots and sealed/opened/resealed state transitions.
- PostgreSQL summary uniqueness is `(trade_date, trade_session, code)`; raw five-second samples remain transient in Redis.
- Keep the current latest Redis bundle available for existing market-indicator readers; finalized limit-board sessions are deleted after PG commit.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Planning skill path under repository did not exist | 1 | Read the installed skill from the user profile path. |
| Docker image build could not fetch `eclipse-temurin:21-jre-jammy` | 1 | PostgreSQL migration had already succeeded; retried after source verification. |
| Release market image build could not fetch `eclipse-temurin:21-jre-jammy` | 2 | Maven and migrations succeeded; current market container was left running because the base-image network fetch failed. |
