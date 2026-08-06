# Market Indicator Implementation Plan

## Goal

Add a complete market-indicator workflow backed by eltdx 1.2.0: Python collection and scheduled monitoring, PostgreSQL persistence under `cynexus_appl_market`, Java domain/application/API exposure, and a new UI page at the second-level route `market/indicator` under the Market navigation.

## Phases

- [completed] Phase 1: Inspect existing market, scheduler, schema, Java API, and UI conventions
- [completed] Phase 2: Define persistence contract and implement Python collection/monitoring
- [completed] Phase 3: Implement Java domain, infra, application, controller, and API contract
- [completed] Phase 4: Implement UI services, route, sidebar entry, page, and components
- [completed] Phase 5: Test, synchronize versions, build, release-stack verify, and clean planning scratch
- [completed] Phase 6: Extend breadth, limit-board, and industry-first-event workflow
- [completed] Phase 7: Test, version-sync, rebuild, and release-verify the extended page

## Decisions

- Use `eltdx` 1.2.0 capabilities already present in the data executor.
- Keep high-frequency/current state in Redis or in-process runtime where appropriate; persist queryable indicator snapshots and confirmed events in PostgreSQL.
- Treat exact historical ticks and current five-level snapshots as separate data contracts; do not pretend 1m bars are tick data.
- Keep business detection/normalization in Python domain/pipeline code and Java domain services; keep AppServices thin.
- Put frontend APIs under feature-specific `src/services/market/` modules, not `lib/api.ts`.
- Keep `sec_stock_basic` and `sec_stock_sector/sec_sector` as the industry master data; persist only per-capture industry indicator aggregates in the new market table.
- Keep market breadth independent from the page's snapshot filter and pagination.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Impeccable context script path missing in repo | 1 | Used installed skill path under `C:\Users\Cs7er\.codex\skills\impeccable` |

## Verification Targets

- Focused Python tests for eltdx normalization, indicator persistence, and event detection.
- Focused Java domain/application/controller tests.
- UI typecheck/build and route smoke check.
- Maven compile and synchronized Maven/UI versions.
- Release Docker stack rebuilt and verified.
