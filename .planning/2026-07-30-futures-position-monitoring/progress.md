# Progress Log

## Session: 2026-07-30

### Phase 1: Requirements & Discovery
- **Status:** complete
- **Started:** 2026-07-30
- Actions taken:
  - Read the repository-provided operating rules from the user message.
  - Read the `planning-with-files` skill instructions.
  - Checked for existing planning context and found related market-data and market-indicator sessions.
  - Checked git status and confirmed a substantial pre-existing dirty worktree.
  - Initialized an isolated analysis plan under `.planning/2026-07-30-futures-position-monitoring/`.
- Files created/modified:
  - `.planning/2026-07-30-futures-position-monitoring/task_plan.md` (created)
  - `.planning/2026-07-30-futures-position-monitoring/findings.md` (created)
  - `.planning/2026-07-30-futures-position-monitoring/progress.md` (created)
- Additional discoveries:
  - Confirmed AkShare `1.18.46` is installed and exposes futures quotation, settlement, contract-info, and position-ranking APIs.
  - Confirmed existing official-exchange-first provider fallback patterns in `cynexus-data`.
  - Confirmed `get_rank_sum('20250718', ['IF'])` returns top-5/10/15/20 long/short aggregates; raw exchange rank functions provide member detail.
  - Confirmed several live/upstream limitations during read-only AkShare probes, so a direct official connector plus fallback and source-health reporting are required.
- Error encountered:
  - A compact live-probe Python command failed syntax validation; no application or data was changed.
  - A PowerShell probe attempted arithmetic on multiple `Select-String` matches; corrected by using the fixed function line number and did not modify any files.

### Phase 2: Planning & Structure
- **Status:** complete
- Actions taken:
  - Defined the daily member-position MVP and separated it from intraday quote/open-interest context.
  - Defined provider, pipeline, storage, Java query, REST, UI, POC, test, release, and commit routes.
  - Documented semantic caveats, data model, source health, and baseline version/worktree risks in `findings.md`.
- Files created/modified:
  - `.planning/2026-07-30-futures-position-monitoring/findings.md` (updated)
  - `.planning/2026-07-30-futures-position-monitoring/task_plan.md` (updated)

### Phase 5: Delivery
- **Status:** complete
- Actions taken:
  - Prepared an approval checkpoint. No implementation files, migrations, version bumps, application processes, or commits were created.

### Plan Revision: Review Corrections
- **Status:** complete
- Actions taken:
  - Replaced the incorrect side-plus-volume fact shape with independent `VOLUME`, `LONG`, and `SHORT` rankings.
  - Added canonical provider selection, capture-batch completeness, coherent futures trade dates, and per-exchange publication/retry rules.
  - Removed directional signals and quote/OI dependencies from MVP.
  - Added contract/variety scope handling, exchange-qualified API identity, saved filters, and `future_instrument` self-selected support.
  - Raised the POC to 20 consecutive trading dates with quantitative provider gates.
  - Aligned the commit plan with per-functional-change Maven/UI version synchronization.
- Files created/modified:
  - `.planning/2026-07-30-futures-position-monitoring/findings.md` (revised)
  - `.planning/2026-07-30-futures-position-monitoring/task_plan.md` (revised)
  - `.planning/2026-07-30-futures-position-monitoring/progress.md` (revised)
- Implementation status:
  - Not started; explicit user start command is still required.

## Test Results
| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| FastCtx availability | Tool catalog search | FastCtx tools available | Not exposed; fallback inspection selected | noted |
| AkShare installed version | Python import | Futures APIs available | `1.18.46`; futures rank/settlement/quote APIs exposed | pass |
| Historical rank aggregate probe | `get_rank_sum('20250718', ['IF'])` | Normalized top-N position rows | 5 rows, 27 fields, top-5/10/15/20 long/short values | pass |
| Official/adapter live probe | CFFEX/DCE/Sina samples | Stable current source | Empty/blocked/mapping limitations observed and recorded | warning |
| Direct AkShare position probe | `2025-07-18`, CFFEX/SHFE/CZCE/DCE/GFEX | Per-exchange raw rank tables | CFFEX 4, SHFE 4, CZCE 104, GFEX 0; DCE `BadZipFile` | partial |
| Futures source/domain unit tests | Synthetic CFFEX/DCE rows | Three independent rank facts; synthetic totals excluded | Added tests for normalization and AkShare exchange boundary | pending |

## Error Log
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-07-30 | FastCtx MCP tools unavailable in current catalog | 1 | Use read-only PowerShell fallback |
| 2026-07-30 | Planning-file patch context mismatch | 1 | Re-read the current file and apply a narrower patch |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | Implementation Phase 1: POC contract and source boundary added; tests pending |
| Where am I going? | Run focused tests, then add isolated per-exchange POC/reporting before schema and UI |
| What's the goal? | Analyze a futures long/short monitoring feature for `cynexus-market` before implementation |
| What have I learned? | See `findings.md`; related market data work and a dirty worktree already exist |
| What have I done? | Added the first futures domain normalization contract, AkShare adapter boundary, and focused unit tests; no schema/UI yet |

## Implementation Session Update

- Phase 6 complete: exchange-isolated AkShare adapter and independent rank normalization are covered by focused tests. The live POC remains partial: CFFEX/SHFE/CZCE have direct historical results, while DCE returned `BadZipFile` and GFEX was empty for the sample.
- Phase 7 complete: PostgreSQL contract/catalog, capture-batch audit boundary, canonical member rank storage, idempotent replacement semantics, data job registry, daily scheduler migration, and Release migration wiring are implemented.
- Data tests: `6 passed`; Python `compileall` completed for `cynexus_data`.
- Version sync: root and all module Maven POMs plus UI package are `1.1.357-SNAPSHOT`.

## Java Query API Update (2026-07-30)

- Added the market domain query port and policy-backed coherent-date, scope, sort, and page semantics.
- Added the infra Repository conversion path and MyBatis queries for selected capture batches, independent `VOLUME`/`LONG`/`SHORT` Top-N aggregation, member ranks, history, and coverage.
- Removed the Spring stereotype from the domain implementation; application configuration is the sole Bean assembly point.
- Added focused domain tests for date fallback, filter/page normalization, empty data, and invalid rank type.
- Verification pending: run the focused Maven test and module compile before committing.

### UI Workflow Update (2026-07-30)

- Added `/market/futures-position` with saved filters, contract/variety scope, exchange/date/Top-N filters, latest ranking table, aggregate structure, coverage state, member detail drawer, and history view.
- Added `future_instrument` self-selected items with exchange-qualified identity and navigation back to the monitor.
- Added the Market sidebar and router entries, with the Java client isolated under `src/services/market/futures-position.ts`.
- Maven full compile at `1.1.357-SNAPSHOT`: `BUILD SUCCESS`.
- UI production build at `1.1.357-SNAPSHOT`: passed; Vite emitted existing unresolved Montserrat runtime-path warnings and a chunk-size warning.
- Futures mapper XML parsed successfully; SQL execution still requires Release PostgreSQL verification.

### Release Verification Update (2026-07-31)

- Commit `56c024ad` delivered the UI workflow, exchange-qualified identity fix, version synchronization, and self-test record.
- Release command completed: `start-release.ps1 -Version 1.1.357-260731-RELEASE -Build -Services market,ui`.
- Versioned `market` and `ui` images were built and the Release containers were recreated successfully.
- Market migrations applied successfully. PostgreSQL contains both futures position tables, and `futures_position_daily` is enabled for `19:00` trading-day execution.
- Release HTTP checks passed with status 200 for latest, coverage, members, and history. With no capture rows, latest returns `ok=false` and an explicit empty-data message; no false snapshot is displayed.
- UI route `/market/futures-position` returned the Release HTML successfully.
- Live non-empty ingestion remains pending because the shared `cynexus-data-job-executor:local` image was created on 2026-07-29 and does not contain `sync_futures_position` (`/internal/jobs/sync_futures_position` returned 404). The shared Data Executor was not restarted, per repository rules.

### Data Executor Completion Update (2026-07-31)

- Rebuilt `cynexus-data-job-executor:local` from the current source and recreated only the Data Executor with `--no-deps --force-recreate`; PostgreSQL, Redis, ClickHouse, Nacos, and Release application containers were not restarted.
- Data Executor became healthy and now exposes `sync_futures_position` with HTTP 200.
- Triggered `sync_futures_position` for `2026-07-30`, provider `akshare`, exchange `CFFEX`, variety `IF`.
- Run `job_run_65f04e18c8bf41a49a09a6506abc8299` succeeded: 84 source rows read, 240 rank rows written, 4 instruments captured.
- Release market API verified the non-empty workflow: latest returned 4 instruments, coverage returned `SUCCEEDED`, member details returned 5 LONG rows for `IF2703`, and history returned 1 daily structure row.
- The default all-exchange sync remains subject to the previously recorded DCE/GFEX upstream limitations; this verification intentionally used the stable CFFEX path.

### Verification Error

- Maven focused test attempt 1 failed before compilation because PowerShell split the unquoted `-Dtest=...` argument and Maven interpreted the class suffix as a lifecycle phase. The next attempt uses a quoted system-property argument.
- Maven focused test attempt 2 accepted the test selector but stopped during dependency resolution because `cynexus-market-common` was not installed; the next attempt will build required reactor dependencies with `-am`.
- Maven focused test attempt 3 built the first reactor dependency but Surefire failed in `cynexus-common-core` because the selected test does not exist there; the next attempt adds `-Dsurefire.failIfNoSpecifiedTests=false` for upstream modules.
