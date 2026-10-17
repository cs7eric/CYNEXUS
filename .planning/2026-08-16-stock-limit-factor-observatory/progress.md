# Progress

## 2026-08-16

- Created an isolated plan for the Stock Limit Factor Observatory.
- Began discovery of the existing Screener, limit-up, market-state, industry, K-line, margin, futures, AI, and Agent contracts.
- Confirmed that the existing Market pulse domain provides a daily persisted limit-up/limit-down/broken-board pool and the data layer already materializes as-of market-state and industry-factor context.
- Confirmed the Pulse API can supply a single-date limit event pool, while the new observatory needs a dedicated multi-date, multi-factor evidence contract. Confirmed the existing Screener Agent tool is the implementation reference for a bounded Market Feign read.
- Confirmed the Market indicator domain provides deterministic market/industry/replay evidence with explicit coverage states, and the Screener DTO provides reusable technical, liquidity, fund-flow and classification fields. The new API will aggregate these under one as-of-date contract rather than generate causal labels from client-side joins.
- Confirmed the existing Screener snapshot can act as the primary per-event technical/classification/fund-flow input and that Market indicator data can provide the matching date's broad market and industry context. Defined the first observatory API as a bounded selected-day cohort read.
- Expanded the contract after requirements clarification: the feature is a generic market-event factor observatory, not a limit-up-only view. It will preserve intraday industry, nine MSI, market/industry cycle, breadth/new-high/new-low, margin, index-path, and future swing-factor evidence as separate time-staged factor families.
- Confirmed the nine MSI components and their weights, plus the exact Market Indicator, margin, and Screener contracts. The implementation can compose domain services behind a dedicated date-bounded observatory service without leaking repositories into application or AI modules.
- Implemented the first governed observatory slice: `GET /api/market/screener/factor-observatory` returns a bounded LIMIT_UP or LARGE_MOVE cohort with separate market-regime, MSI-9, breadth/new-high/new-low, index-path, industry, materialized-intraday-coverage, margin, and pre-event technical evidence families.
- Added a Feign boundary and a bounded autonomous Agent read tool (`market.event_factor_observatory`). The tool permits only a selected date, two cohort types, a 0-30% large-move threshold, one symbol, and up to 100 rows.
- Added the Screener's `因子观察` tab with event-pool controls, source/as-of and coverage states, editable typed-condition handoff, and Trade Review navigation for an event row.
- Verified focused Market-domain and Agent-handler tests, Market and AI compilation, and the UI production build. The repository-wide UI lint remains red because of pre-existing unrelated React diagnostics; the new UI compiles successfully.
- Rebuilt and recreated the full Release stack at `1.1.400-260816-RELEASE`. Market, AI, Gateway, and UI containers are running.
- Verified the public Release route `GET /api/market/screener/factor-observatory?tradeDate=2026-08-14&cohort=LIMIT_UP&limit=1` through the UI gateway: HTTP 200 with 63-event population metadata, all nine MSI components, breadth/new-high/new-low, index path, industry/intraday coverage, margin, and pre-event event evidence.
- Verified governed Agent registration in PostgreSQL: `market.event_factor_observatory` is enabled, AVAILABLE, read-only, requires no confirmation, and appears under `Agent / market`.
- Recorded that the first endpoint response takes approximately 26 seconds because it builds full daily Screener snapshots for the event and preceding sessions. This does not invalidate the bounded response, but a snapshot cache/narrow pre-event lookup is the next performance improvement.
- Implemented governed opt-in AI factor synthesis under `market_event_factor_analysis`. The model receives only the deterministic observatory evidence envelope and must return source-referenceable, non-causal hypotheses and caveats; no autonomous AI execution or trading conclusion is permitted.
- Added the API route `POST /api/ai/market-event-factors/analysis`, a DB-managed `market_event_factor_analysis` prompt and enabled usage binding, bounded prompt/response execution logging, focused policy tests, and a Screener-local provider/model selector plus explicit `综合` control.
- Optimized `LIMIT_UP` observation by deriving the event population from the persisted limit-emotion pool and loading only the prior-trading-day Screener snapshot for pre-event evidence. `LARGE_MOVE` still requires its event-day Screener snapshot.
- Rebuilt and recreated the full Release stack at `1.1.400-260816-RELEASE` after the AI and Market changes. All Release containers are running; Maven and UI versions remain `1.1.400-SNAPSHOT`.
- Verified the new prompt binding in PostgreSQL (`market_event_factor_analysis|true|market_event_factor_analysis|v1`) and the public deterministic observatory route through the Release gateway. The route returned HTTP 200 for the 2026-08-14 limit-up cohort with 63 events and the expected evidence families.
- Verified the AI request reaches the new controller and provider runtime. The configured `openai_compatible` upstream returned HTTP 502 with an unstructured body, so no model synthesis result can be verified until a healthy provider/model route is selected or the upstream is repaired.
- Began replacing the incremental Phase 7 plan with a directly executable enterprise delivery blueprint after the user requested a plan that can be replicated during implementation. No business code was changed.
- Completed `implementation_plan.md`: a phased, directly executable blueprint covering reusable project capabilities, target architecture, point-in-time data contracts, factor catalog, event providers, study statistics, controls/outcomes, Factor Set lineage, APIs, frontend workbench, governed AI, Agent tools, tests, performance, release, rollback, staffing, and end-to-end acceptance. No business code was changed.
- Corrected the blueprint after user review: predefined technical indicators are now primitive observations only. The target system materializes event-aligned raw sequences, discovers adaptive state transitions and motifs (including variable-duration low-volume regimes followed by learned-range volume bursts), separates common/subgroup/rare-special/unexplained samples, validates discoveries against controls and chronological holdouts, and promotes only versioned recipes. No business code was changed.

## 2026-08-16 Enterprise Implementation Start

- User authorized implementation of Phase 0 through Phase 7.
- CodeGraph index is current. The existing code only provides the legacy single-day `MarketEventFactorObservatory` contract and cannot persist studies, controls, sequences, discoveries, or factor sets.
- Existing uncommitted changes are preserved as the implementation baseline; Phase 0 contract work starts additively.
- Added the additive `20260816_create_market_factor_research.sql` migration and registered it with the Release market migration bundle. It creates point-in-time daily snapshots, owner-scoped studies/samples/sequences, immutable discovered factors, factor-set versions, and additive strategy/candidate lineage references.
- Added pure Market domain research contracts and `MarketFactorResearchPolicy`; the initial tests enforce event-day daily-feature leakage rejection and bounded study configuration.
- Explicitly marked legacy `screeningHints` as deprecated in the API/UI while retaining it for one compatibility period.
- `MarketFactorResearchPolicyTest` and the Market application-controller compilation passed. The UI build was started twice but did not return a final result inside the tool's 30-second execution window; do not treat it as passed.
- Targeted ESLint passed for the changed Screener service and observatory tab. Maven/UI version synchronization remains `1.1.400-SNAPSHOT`.
- Began Phase 1 with a separate `EventReplayDomainService`. It composes strict-date stock 1m bars and existing materialized market/industry replay evidence around an explicit `anchorAt`, propagating partial coverage rather than silently falling back to another date. Market domain compilation passed.
- Added `GET /api/market/screener/event-replay` with an API DTO, application mapping and Spring assembly. It is bounded to one code/trade date and returns stock minute bars plus materialized market/industry replay status. Market application-controller compilation passed.
- Added `fetchMarketEventReplay` and its typed response contract to the Screener frontend service. Targeted ESLint passed.
- Added `materialize_market_factor_snapshots` JobRunner job, pipeline and PostgreSQL repository. The job materializes versioned daily primitive snapshots from `daily_raw`, supports bounded ranges, checkpointing and dry-run; the dry-run passed without accessing storage.
- Corrected the Phase 1 replay boundary so stock 1m bars are restricted to the requested Shanghai trade date and `anchorAt` window. The replay now returns an explicit data cutoff and source-level coverage rather than treating an arbitrary first page of minute bars as the event window. Added focused leakage/anchor tests; they passed with the existing research-policy tests.
- Extended the Phase 2 schema with point-in-time THS/TDX membership and event-intraday snapshot tables. The daily primitive materializer now reconciles its durable row count against `daily_raw`, records the result in its checkpoint, and returns a failed result rather than silently publishing incomplete coverage. Four focused Python tests and the JobRunner dry run passed.
- Began Phase 3's asynchronous Study contract with an explicit domain lifecycle policy. It enforces `QUEUED -> RUNNING -> terminal`, only permits retries from terminal states, and prevents a leakage-failed run from being treated as successful. The focused Market-domain suite now has seven passing tests.
- Added the owner-scoped Study domain service and persistence port. It validates bounded research configs, provides canonical idempotent queueing, owner-scoped lookup/cancellation, and terminal-only retry semantics. The Phase 3 domain suite has eight passing tests; database adapter, worker and public API remain next.
- Added PostgreSQL/MyBatis persistence for owner-scoped Study creation, retrieval, durable queued claiming, cancellation and completion; the migration now records leakage-check status. Added `POST /api/market/factor-studies` (202), owner-scoped status lookup, cancel, and retry endpoints. Market infrastructure and application-controller compilation pass. The async computation worker and Cohort/Discovery engine are not implemented yet.
- Added the Phase 3 Cohort Provider contract, duplicate-safe registry, and a versioned `LIMIT_UP_CLOSE` provider backed only by persisted `market_limit_daily_stocks`. It produces frozen source facts and an explicit Shanghai close anchor rather than inferring an event from live state. The focused domain suite now has 10 passing tests. An initial null source-fact test failure was corrected by omitting unavailable source values before immutable-copying the facts map.
- Added a pure pre-event `MarketFactorControlMatchPolicy`: it excludes event/ST samples, requires same trade date and market, matches industry/capital/liquidity buckets first, and records each controlled fallback rather than silently relaxing the population. The focused suite now has 11 passing tests.
- Added deterministic statistical comparison primitives (support/counter counts, lift, effect confidence interval and Benjamini-Hochberg correction) and materialized Cohort providers for `LIMIT_UP_TOUCH` and editable-range `LARGE_MOVE_CLOSE`. Both use `trade_review_security_daily_fact`; the touch provider preserves `first_limit_up_at` and the close provider freezes the requested range in each source sample. Added the Market infra adapter/mapper and Spring registry for all three first-version events. Focused domain tests now cover 15 cases; infra and controller recompilation completed from the changed sources, with only existing Lombok warnings.
- Added the durable scheduled Study dispatcher. It claims one queued study, resolves only its registered point-in-time Cohort Provider, persists immutable event samples, and returns `NO_DATA` or explicit `PARTIAL` rather than falsely declaring research success while control/sequence/discovery stages remain pending. Added study-sample mapper support and execution tests. The focused domain suite has 17 passing tests; Market application-controller and infra Maven compilation passed.
- Added owner-scoped study sample retrieval and persisted progress counts, plus the canonical research URL aliases: `/api/market/screener/factor-research/studies` and `/api/market/screener/factor-research/events/{tradeDate}/{code}/replay`. Legacy routes remain intact for the compatibility period. The focused Study tests pass and updated Market infra/controller classes compiled.
- Added owner-scoped paginated Study listing on both the legacy and canonical Study routes. Market application-controller compilation passed after the API/repository changes.
- Added the event-aligned daily sequence policy: it hard-rejects event-day values, verifies primitive availability, assigns stable negative offsets, and stores raw plus robust median/MAD-normalized values. Its focused tests pass with the Study/Cohort/control/statistics suite.
- Added the versioned factor-promotion policy. It keeps insufficient samples/coverage as `EXPLORATORY`, rejects unstable or non-positive chronological holdout evidence as `WEAK`, and permits promotion only after q-value, train/validation, monthly, Regime and outcome stability gates. The Phase 3 focused domain suite has 13 passing tests.
- Added the Phase 2 THS membership materializer. It selects each industry's historical source snapshot as of the effective trade date, replaces only that effective THS snapshot, retains source snapshot date/coverage, and writes a checkpoint. Registered the JobRunner job and corrected the prior factor snapshot YAML indentation so `jobs.yaml` parses. Six focused materializer tests, YAML parsing and the membership job dry run passed.
- Added chronological train/validation splitting that preserves same-day cohorts and forbids randomized or overlapping temporal holdouts. The Phase 3 focused domain suite now has 14 passing tests.
- Added the four deterministic raw-sequence discoverers and their bounded registry engine: change point, shape cluster, sequential low-to-recovery rule, and interpretable metadata subgroup. They emit learned recipes/bounds and event/control support rather than fixed technical-indicator conclusions. The focused Phase 3 suite has 15 passing tests.
- Registered the four discoverers in the Market Spring configuration and extended the additive research migration with durable per-study factor result and pattern-cluster tables. Market application-controller compilation passed.
- Added the post-event outcome evaluator with bounded horizons, stock/benchmark return, excess return, MFE/MAE and explicit missing-horizon status. It stays outside control matching and discovery inputs. The Phase 3 focused domain suite now has 16 passing tests.

## Errors

| Error | Attempt | Resolution |
| --- | --- | --- |
| Maven parsed the unquoted `surefire.failIfNoSpecifiedTests` property as a lifecycle phase. | Initial focused test | Retried with quoted Maven properties; test passed. |
| UI build did not return a final exit status inside the command window. | Two `pnpm build` attempts | Left existing Node processes untouched; final UI verification remains pending. |

## 2026-08-17 Continuation

- Added the Phase 2 bounded event-intraday materializer (`materialize_market_event_intraday_snapshots`). It freezes stock `close`, `volume`, and `amount` 1m primitives around an explicit anchor into `market_event_intraday_snapshot`, records a checkpoint, supports dry-run, and rejects coverage mismatches. Market and THS/TDX industry scopes are deliberately not fabricated; their absence remains explicit partial coverage pending historical point-in-time source materialization.
- Registered the JobRunner entry and job configuration. Eight focused materializer tests passed, `jobs.yaml` parses, and the new job dry-run passed.
- Hardened Phase 3 cohort freezing: event anchors later than the Study's immutable `dataCutoffAt` now terminally fail the Study rather than admitting future data. The 18-test focused Market domain suite passed.
- Extended the daily primitive snapshot payload with raw OHLCV, amount, turnover, return, amplitude, market-cap fields, and normalized candlestick shape observations. These remain primitive facts and do not encode a hard-coded discovered factor. Seven focused Python tests passed.
- Corrected daily primitive availability semantics: `available_at` is now the source trade date's Shanghai 15:00 close boundary, not the materialization job's execution time. This keeps historical as-of leakage checks stable. Five focused snapshot tests passed.
- Corrected event-intraday primitive availability semantics: each 1m observation uses its own observed minute as `available_at`, preventing backfill timing from changing anchor-time visibility. Three focused intraday tests passed.
- Event-intraday materialization now returns a stable `STOCK_1M_UNAVAILABLE` partial-data failure instead of silently completing an empty snapshot. Four focused intraday tests passed.
- Continued Phase 3 with point-in-time matched controls. The Study dispatcher now reads only versioned materialized daily snapshots strictly before each event date and at or before the study cutoff, excludes event/ST samples, buckets pre-event float market cap and liquidity, records every fallback quality/distance, and persists control samples separately from the frozen event cohort. The Study remains `PARTIAL` until sequence persistence, discovery, statistics, outcomes, and chronological validation are complete.
- Added the PostgreSQL adapter query for as-of snapshots, including historical membership where available and conservative ST handling when stock metadata is missing. Four focused execution-domain tests passed; Market infra and application-controller compilation passed.
- Continued Phase 3 with durable event-aligned daily sequence freezing. After event/control samples are persisted, the dispatcher reads only pre-event primitive observations (`close`, `volume`, `amount`, `turnover`, `return_1d`), applies the domain MAD-normalization policy, and saves raw value, normalized value, offset, observation availability and source under the immutable sample identity. The execution remains `PARTIAL` until candidates, statistics, outcomes and chronological validation are persisted.
- Market domain focused tests and the Market infra/application-controller compile passed after the sequence persistence port, mapper and adapter changes.
- Continued Phase 3 by wiring the registered deterministic discoverers into Study execution. Frozen normalized sequences are grouped by event/control sample, passed through the bounded discovery engine, compared with deterministic support/lift/effect statistics, and persisted as immutable per-study factor results with `EXPLORATORY` verdicts until q-value, chronological holdout, regime, outcome and promotion gates are available. Empty controls now yield an explicit no-discovery result instead of a divide-by-zero failure.
- Focused execution tests passed and Market infra/application-controller compilation passed after adding factor-result persistence.
- Continued Phase 3 with bounded post-event outcome evaluation. The study worker now reads event-close and subsequent point-in-time closes plus the persisted HS300 benchmark series, evaluates configured 1/3/5/10/20-session horizons with explicit unavailable results, and stores outcome snapshots on immutable event/control samples. Added the idempotent outcome-column migration and Release migration registration.
- Focused execution tests passed; Market infra/application-controller compilation passed after outcome mapper and repository changes.
- Added the owner-scoped `GET /api/market/screener/factor-research/studies/{studyId}/discoveries` read. It returns only persisted deterministic recipes, learned bounds, support/lift/effect statistics, validation state, evidence and verdict; it does not expose raw samples or allow AI/client-side recomputation.
- Added typed frontend research service contracts for creating/listing asynchronous factor studies and reading owner-scoped discoveries. Targeted ESLint passed. The research workbench view itself remains to be connected to these calls.
- Continued Phase 3 validation and Study APIs. Discovery persistence now records adjusted q-values, chronological split status, coverage/effect confidence intervals and non-pending promotion verdicts; a fully materialized study may transition to `SUCCEEDED`, while missing controls, outcomes or evidence remains `PARTIAL`.
- Added owner-scoped Study cluster, discovery-detail, explicit candidate-promotion and outcome read routes. Promotion is user initiated and rejects every non-candidate result; it does not create a strategy.
- Added the Screener-local `Research` workbench tab. It queues bounded studies, polls queued/running status, renders persisted discovery verdict/lift/q-value evidence, has loading/error/empty states and does not calculate conclusions in the browser.
- Market infra/application-controller compilation and the targeted Screener lint passed. The production UI build now passes after the Research workbench addition.
- Began Phase 4 Factor Set delivery. Added owner-scoped Factor Set definition/version contracts, create/update/list/get/validate/promote APIs and PostgreSQL adapter over the pre-created schema. Versions are immutable rows; update creates a new `DRAFT`, validation is explicit, and promotion atomically marks the reviewed version `PROMOTED` and activates it. Corrected the version lookup to resolve the latest draft/validated version rather than only an already-active one, so the validation/promotion flow is reachable.
- Continued Phase 6 Agent governance with `market.factor_study` v1.0.0. The strict owner-scoped read uses the signed `AgentExecutionContext` identity, only permits bounded `offset`/`limit`, exposes immutable study metadata (including cutoff, leakage status, coverage and warnings), and cannot create/retry/cancel/promote studies or strategies. The Market Feign contract and AI infra compilation pass.
- Added `market.event_replay` and `market.factor_set_validation` v1.0.0 Agent reads. Replay is bounded to one six-digit symbol/date/anchor and 30–300 minute rows; Factor Set validation is signed-owner scoped and read-only. All three new tools have strict schemas, source/freshness metadata and no mutation path.
- Rebuilt and force-recreated the full Release stack at `1.1.400-260817-RELEASE`. Gateway health returned HTTP 200 and PostgreSQL governance records confirm `market.event_replay`, `market.factor_study`, and `market.factor_set_validation` are enabled and AVAILABLE at v1.0.0.
- Resumed execution assessment: Phases 0–2 are complete; Phases 3, 5, 6 and 7 are partial; Phase 4 currently has only backend Factor Set version APIs. The next implementation slice is the typed Factor Set editor and explicit validation/promotion workflow, without auto-creating strategies.
- Completed the first Phase 4 UI slice: the Research tab now provides a typed Factor Set expression editor (AND/OR, typed field, condition value and positive weight), guarded create/update with the backend optimistic version, explicit validate/promote actions, and discovery-to-expression selection. It preserves Study IDs as strings so Snowflake identifiers are not rounded in the browser, and it does not create strategies implicitly.
- Targeted ESLint and `pnpm build` passed for the Factor Set editor. Build retains the pre-existing warnings for missing Montserrat font assets and large chunks.
- Completed the Phase 4 expression-policy slice. The domain now accepts only schema-v1 typed expressions, bounded AND/OR clauses, supported operators, valid interval/value fields and safe positive weights. Validation can only review the latest Draft version, and its response retains that exact version's identity and expression. Two focused Factor Set domain tests plus Market application-controller compilation passed.
- Continued Phase 5 wiring: the Research tab now reads persisted owner-scoped outcome rows alongside discoveries and exposes a compact per-event horizon/excess-return validation summary. The browser remains a renderer only; all outcome calculations come from the deterministic Study result. Targeted ESLint and the prior production build pass.
- Corrected Phase 3 chronological validation: every deterministic discoverer now emits its supporting event/control sample IDs, and the study execution computes date-disjoint train and validation effects from those IDs instead of marking any split as passed merely because two dates exist. Validation evidence now includes `trainEffect`, `validationEffect`, and explicit status, with support references retained for audit. Five focused discovery/execution tests passed.
- Added separate monthly and regime stability aggregation. Monthly partitions now require positive effect across at least two YearMonth partitions; regime partitions only run when persisted market/industry regime labels exist, otherwise validation explicitly reports `UNAVAILABLE`. Outcome stability is also explicitly `UNAVAILABLE` until persisted outcome rows are joined to discovered-factor support rather than inferred from sample support. Focused Study execution tests pass.

## Verification

| Check | Result |
| --- | --- |
| Planning scope isolated from existing root planning files | Passed |
| Full Release deployment (`1.1.400-260816-RELEASE`) | Passed |
| Public observatory API through Release UI gateway | Passed, HTTP 200 |
| Agent capability governance record | Passed |
| First endpoint latency | ~25.8 seconds for one limit-up event; follow-up performance work required before high-frequency use |
| Full Release deployment after AI synthesis change (`1.1.400-260816-RELEASE`) | Passed |
| Maven/UI version synchronization | Passed (`1.1.400-SNAPSHOT`) |
| AI prompt/usage binding | Passed (`market_event_factor_analysis`, enabled, `v1`) |
| Governed AI controller and provider route | Reached; initial HTTP 502 was followed by a successful runtime check, with high provider latency still requiring separate monitoring |

## 2026-08-17 Strict Agent output-contract hardening

- Replaced the `market.event_factor_observatory` Agent output schema's unbounded `object` array entries with concrete nested schemas for factor families, metrics, event evidence and deprecated compatibility hints. The capability version is now `1.1.0`.
- Added a regression assertion that prevents the broad `marketFactors.items: { type: object }` schema from returning.
- Focused verification passed: `mvn -pl cynexus-ai/cynexus-ai-infra -am "-Dtest=MarketEventFactorObservatoryAgentToolHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.

## 2026-08-17 Phase 3 outcome-stability continuation

- Study execution now computes and persists outcome snapshots before factor-result persistence, then evaluates each discovered candidate against its supporting event/control samples across the configured horizons.
- Validation records `outcome=PASSED|FAILED|UNAVAILABLE`; promotion receives the actual outcome-stability gate instead of a hard-coded `false`.
- Focused verification passed: `mvn -pl cynexus-market/cynexus-market-domain -am "-Dtest=MarketFactorStudyExecutionDomainServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` (4 tests).
- Phase 3 remains incomplete because persisted market/industry Regime labels and their stability partitions are still unavailable.
- Added optional `market.state` / `industry.phase` extraction from the point-in-time primitive payload into matched-control metadata; missing labels remain null and are never inferred. Added a regression test for label propagation. Market domain focused suite now passes 5 tests; Market domain/infra compilation passes.
- Extended the point-in-time universe query to prefer persisted primitive labels and otherwise join the historical `trade_review_trend_phase` market/THS-industry materializations as-of the snapshot date. No current membership or regime is backfilled when the historical row is absent; the validation status remains `UNAVAILABLE` in that case.

## 2026-08-17 Phase 4 strategy lineage continuation

- Strategy save requests and active version responses now carry `sourceFactorStudyId` and `sourceFactorSetVersionId`.
- User strategy version persistence writes/reads the existing lineage columns, preserving immutable version provenance through create and update.
- Market domain, infrastructure and application-controller compilation passed with `1.1.400-SNAPSHOT`.
- Confirmed the existing backtest `strategy_version_id` and watchlist `sourceCandidateId -> strategy_run -> strategy_version` chain preserves the same lineage without duplicating study identifiers in downstream rows; the Phase 4 lineage item is now tracked as complete.

## 2026-08-17 Phase 4 promoted recipe evaluator continuation

- Factor Set screen preview now resolves recipes only from the owner-scoped source study and `PROMOTED` factor results, evaluates bounded pre-trade primitive sequences point-in-time, and reports `PARTIAL` with missing-sequence warnings rather than fabricating matches.
- Strategy run and symbol evaluation now use the same promoted Factor Set evaluator when the immutable strategy version carries `sourceFactorSetVersionId`; primitive-only strategies retain the existing screener path.
- Focused strategy/backtest/Factor Set tests passed (9 tests); Market domain compilation passed.
- Strategy health now reports source-study factor research availability alongside snapshot coverage, signal density, freshness, and recent excess performance; existing candidate-to-watchlist performance/review persistence chain remains intact.

## 2026-08-17 Phase 5 research workbench continuation

- Added bounded Factor Set screen-preview client/rendering, study samples, persisted discovery-detail drawer, and six explicit research-workspace views with keyboard-visible tab semantics and narrow-pane horizontal fallback.
- Focused UI lint and TypeScript build passed; production UI build passed (with existing missing Montserrat-font and chunk-size warnings).
- Added a bounded owner-scoped `/studies/{studyId}/sequences` read endpoint and UI sequence small-multiple rendering from persisted point-in-time sequence snapshots; Market controller/infra/domain compilation passed.
- Research workspace now persists selected study and active sub-view in the URL, while queued/running jobs continue bounded polling and `PARTIAL` is rendered as an explicit job state.
- Added study-job pagination, selected-study URL restoration, and explicit cancel action for queued/running jobs; focused UI lint and TypeScript checks remain green.
- Discovery workbench now exposes persisted clusters, method/change-point labels, all requested discovery-scope filters including `UNEXPLAINED`, event/control samples, sequence small multiples, and a detailed persisted-evidence drawer.

## 2026-08-17 Phase 3 regime validation closure

- Event samples now receive only the matching historical materialized market/industry Regime labels from their pre-event snapshot, just as controls do; no regime is inferred when the point-in-time snapshot is absent.
- This makes chronological and Regime stability evaluation operate over comparable event/control partitions rather than control-only labels. The focused execution suite passed 5/5.

## 2026-08-18 Phase 5 validation workbench continuation

- Replaced the event-only outcome list with a historical-validation surface that aggregates persisted event outcomes by 1/3/5/10/20-session horizon, keeps outcome coverage distinct from average benchmark-relative excess return, and renders the persisted chronological/monthly/Regime/outcome gates for every discovery.
- The UI remains a renderer: it performs only display aggregation over API-provided outcome snapshots and never recomputes Factor Study statistics or validation verdicts.
- Focused ESLint and the production UI build passed. The build retains pre-existing missing Montserrat font and chunk-size warnings.

## 2026-08-18 Phase 6 persisted-study AI synthesis

- Added `market_factor_study_synthesis`: an owner-scoped AI route that reads only persisted Study metadata and deterministic discovery recipe/statistics/validation/evidence through the Market Feign boundary.
- The domain rejects non-terminal studies and failed leakage checks, bounds model input, validates strict evidence references, records bounded execution metadata, and exposes no mutation capability.
- Added the DB-managed v1 prompt, binding, Provider capability metadata, Release migration registration, and a Research validation-panel Provider/Model selector with an explicit synthesis action.
- Maven reactor compilation for the Market API and AI application controller passed. Focused UI ESLint and production build passed.

## Errors

| Error | Resolution |
| --- | --- |
| Planning helper did not create a slug directory on PowerShell and skipped unrelated root files. | Created the scoped plan manually under `.planning`. |
| Initial Release API request returned HTTP 502. | The gateway queried before Market service registration completed; retry after startup returned HTTP 200. |
| AI factor-synthesis request returned a provider error. | The new route returned a governed `AI_PROVIDER_ERROR`; AI service logs confirm the `openai_compatible` upstream emitted HTTP 502 with no structured detail. |
| First targeted UI lint after adding the editor reported an unsafe manual callback dependency. | Changed the dependency to the full selected Study object; targeted ESLint and the UI build then passed. |
| Direct Maven test from the Market domain module could not resolve its sibling Market common artifact. | Re-ran the exact test through the root Maven reactor with `-pl ... -am`; it passed. |
| First outcome-panel lint rejected synchronous state clearing in an Effect. | Removed the redundant clearing; the selected Study gates rendering and focused ESLint now passes. |
| First focused lint command passed an unsupported `--filter` argument through to ESLint. | Re-ran the direct file check with `pnpm exec eslint src/views/cynexus-screener/factor-research-tab.tsx`; it passed. |

## 2026-08-18 Phase 7 continuation

- Verified the full Release stack is running `1.1.400-260818-RELEASE`; Gateway health and the Release UI root both return HTTP 200. The persisted `market_factor_study_synthesis` prompt and enabled usage binding are present in PostgreSQL.
- Added focused governed-AI synthesis tests covering passed-leakage enforcement, malformed model output rejection, and reference filtering. The focused Maven test passed (3 tests).
- Hardened the daily factor-snapshot job for large backfills: PostgreSQL upserts are bounded by `batch_size`, and the 504-trading-day job is detached from the HTTP request lifecycle so a client timeout cannot cancel its durable JobRunner execution. Focused Python tests passed (7 tests).
- Rebuilt and recreated the shared Data Executor image. The first synchronous 504-day attempt was marked `expired` by startup recovery after the executor restart; no snapshot rows were committed. The replacement durable run `job_run_1bfa08f7603142bca1e2fa0ab0cd98bd` was accepted asynchronously for `2024-07-19` through `2026-08-17` and remains in progress. Completion, coverage reconciliation, performance measurement, and final Phase 7 smoke evidence remain pending.

## 2026-08-18 Phase 7 verification closure

- The 504-trading-day backfill completed successfully as `job_run_1bfa08f7603142bca1e2fa0ab0cd98bd`: 2,804,333 source rows read, 2,804,333 rows written, 504 distinct trade dates, and 100% source/materialized reconciliation.
- Added a bounded five-minute application cache for deterministic observatory responses (128-entry cap). Before caching, repeated Release reads were approximately 5.2-14.2 seconds; after Release rebuild/recreate, a cold read was 8.6 seconds and the same-key warm read was 17ms.
- Rebuilt and recreated the full Release stack at `1.1.400-260818-RELEASE`; all application containers are running. Gateway health and the Release UI root return HTTP 200. The AI synthesis route is reachable and returns a governed unavailable-study error when no persisted study exists; no ungrounded model output is produced.
### Phase 0/1/2/5/6 continuation (2026-08-18)

- Added contract snapshot assertions covering observatory, replay, study, strategy lineage, watchlist lineage and deprecated compatibility hints.
- Added `phase0-baseline.md` with frozen event/time/API vocabulary, Release latency baseline and 504-day materialization coverage evidence.
- Verified and checked the extracted EventReplay domain service, point-in-time replay DTO coverage, Trade Review navigation, six research workspace views, accessibility roles, provider governance metadata and persisted-study-only AI synthesis.
- Verified the 120-session backfill requirement is exceeded by the 504-trading-day reconciliation evidence.
- Added a persisted-study quality dashboard view with leakage, sample coverage, feature version, cutoff and active-warning alert states; TypeScript validation passes.
- Extended the daily primitive materialization reader with a 400-day history window and reusable 3/5/10/20/60/120/252-day returns, MA5/10/20/60 and volume-vs-average observations; focused Python tests pass.
- Moved the Factor Research workspace into the right resizable results pane; the left pane retains configuration/navigation and the research editor no longer renders at the lower-left.
- Completed the first primitive/sequence path: daily materialization now persists rolling return, moving-average and volume-relative observations; Market Study reads each as event-aligned raw and MAD-normalized sequences; intraday snapshots preserve 1m OHLCVA observations. The materializer does not infer factor verdicts.
