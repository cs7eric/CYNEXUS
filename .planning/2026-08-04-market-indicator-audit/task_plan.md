# Task Plan: Market Indicator Pool-Semantics Fix

## Goal

Implement the audited Market Indicator fixes while preserving the user's explicit product decision: the monitored universe remains the recent limit-up/limit-down historical pool, not the full market.

## Phases

- [completed] Phase 1: Locate the module, intended product behavior, and applicable guidance
- [completed] Phase 2: Trace data ingestion, calculation, persistence, API, and UI consumption
- [completed] Phase 3: Inspect tests, configuration, scheduling, runtime assumptions, and failure modes
- [completed] Phase 4: Identify root causes and rank remediation options
- [completed] Phase 5: Verify workspace state and deliver the read-only report
- [completed] Phase 6: Define pool-scoped contracts and inspect impact before editing
- [completed] Phase 7: Fix Python capture quality, event/limit semantics, and atomic persistence
- [completed] Phase 8: Fix Java query semantics, API counts, industry first-event/relation behavior, and Agent guidance
- [completed] Phase 9: Fix UI pool wording, rapid-event filtering, freshness/partial states, and payload behavior
- [completed] Phase 10: Run focused tests, compile/build, version checks, and inspect Release impact

## Decisions

- Preserve the historical limit-up/down pool and make all derived indicators explicitly pool-scoped.
- Do not start source-tree Java, Vite, Python, or deprecated Dev applications.
- Use CodeGraph before direct source inspection, then inspect only relevant files.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| `codegraph search` is not a valid command | 1 | Checked CLI help; use `codegraph query` instead. |
| Repository grep crossed unreadable local ClickHouse data directories and returned exit 1 | 1 | Results before those directories were usable; subsequent inspection is restricted to explicit source/config paths. |
| Initial scheduler-status SQL referenced non-existent timestamp columns | 1 | Inspected `information_schema`; use `last_run_at`, `last_run_status`, `last_run_error_message`, and run-history `started_at`/`ended_at`. |
| `git log` returned no history for the module | 1 | Verified the entire market-indicator implementation is currently untracked, so there is no committed history to inspect. |
| Search for advanced eltdx indicator APIs returned no source matches | 1 | Treat as evidence that the originally researched APIs were never integrated; no retry needed. |
| Planning skill path under the repository was absent | 1 | Loaded the installed planning skill from `C:\Users\Cs7er\.agents\skills\planning-with-files`. |

## User Decisions

- Keep the monitored universe as the recent historical limit-up/limit-down pool.
- Do not expand this feature into a full-market collector.
- Correct the semantics and reliability of the pool-scoped indicators instead of relabeling them as full-market measurements.
