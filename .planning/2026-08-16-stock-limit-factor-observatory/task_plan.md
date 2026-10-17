# Task Plan: Stock Limit Factor Observatory

## Goal

Add an extensible governed Screener tab that studies market events (initially limit-up and large-move stocks) through multi-source, multi-timescale evidence; produces bounded reusable factor hypotheses; supports factor-driven screening/replay; and exposes safe factual reads to the autonomous Agent.

## Current Phase

Phase 10 / Phase 0 contract freeze and baseline is in progress.

## Phases

### Phase 1: Discovery and Contract Design
- [x] Map the existing limit-up, market-state, industry, price-volume, margin, futures, Screener, strategy, and Trade Review contracts.
- [x] Define an extensible event/factor contract with event, pre-event, intraday-industry, market-regime, and post-event evaluation stages.
- [x] Map the nine MSI factors and other market/industry sources into independent evidence families with coverage and counter-evidence semantics.
- [x] Define the bounded observatory API and factor-evidence model, including date/as-of semantics and no-causality safeguards.
- [x] Record existing data gaps and establish the MVP scope.
- **Status:** completed

### Phase 2: Market Domain and API
- [x] Implement a typed Market domain read for daily event pools and per-stock multi-factor evidence.
- [x] Implement deterministic factor aggregation, coverage, and warning rules in the domain layer.
- [x] Keep factor providers pluggable so future regime, industry-cycle, swing, or alternative event factors can be added without changing the API contract.
- [x] Add API DTOs, controller/app-service mapping, infrastructure adapters, and focused tests.
- **Status:** completed

### Phase 3: Agent and AI Capability
- [x] Add a bounded read-only Agent tool for event-pool and factor-evidence queries.
- [x] Verify Agent governance schemas and tool route resolution.
- [x] Deliberately defer model-backed factor synthesis: the implemented capability returns inspectable deterministic evidence and does not make ungrounded causal claims. A future opt-in synthesis must complete the full AI prompt/provider/binding/migration contract.
- **Status:** completed

### Phase 4: Screener Observatory UI
- [x] Add the factor-observatory tab, event-pool controls, evidence view, and factor selection handoff.
- [x] Add loading, empty, stale, error, and source/as-of states; preserve the existing deterministic Screener result workflow.
- [x] Connect factor hypotheses to editable criteria and Trade Review; strategy, watchlist, and backtest remain available through the existing Screener workflow instead of being auto-created from unvalidated evidence.
- [x] Deliberately omit an AI button until a fully governed model-backed synthesis capability exists.
- **Status:** completed

### Phase 5: Verification and Release
- [x] Run focused Market and Agent tests plus Maven compilation.
- [x] Run UI build and version synchronization checks; record the pre-existing unrelated lint diagnostics.
- [x] Deploy the full affected Release stack and validate the governed API, Agent tool, and UI route. No model-backed AI route was added.
- **Status:** completed

### Phase 6: Governed AI Factor Synthesis
- [x] Inspect the existing AI routing, prompt, execution-log, and provider-selection patterns.
- [x] Add an explicit opt-in AI synthesis capability that only summarizes the deterministic observatory evidence and returns structured, non-causal factor hypotheses.
- [x] Add DB-managed prompt, usage binding, provider/model routing metadata, validation, observability, focused tests, and the Screener-local route selector.
- **Status:** completed

### Phase 7: Historical Validation and Performance
- [ ] Add bounded historical outcome evaluation for an event cohort and factor conditions, with date-window and horizon controls.
- [ ] Connect validated factor sets to the existing strategy/backtest/watchlist workflow without automatic mutations.
- [x] Reduce initial observatory latency through a dedicated narrow event evidence path for the LIMIT_UP cohort.
- **Status:** superseded by the validated enterprise scope in Phase 10

### Phase 8: Verification and Release Extension
- [x] Run focused AI/Market tests, compilation, UI build, version synchronization, and Release deployment.
- [x] Verify the AI route reaches the governed controller, deterministic evidence route, prompt/binding migration, and Release UI flow.
- **Status:** superseded by the Phase 10 implementation and acceptance plan; the AI route was later observed succeeding, while historical validation remains unimplemented

### Phase 9: Enterprise Re-plan
- [x] Re-audit the current observatory against the original event-factor research requirements.
- [x] Separate reusable foundations from missing research-domain capabilities.
- [x] Define the target architecture, temporal semantics, data materialization, domain contracts, API, UI, AI, Agent, testing, release, and acceptance gates.
- [x] Publish the directly executable implementation blueprint in `implementation_plan.md`.
- [x] Correct the blueprint from predefined-indicator validation to event-aligned sequence discovery with adaptive regimes, change points, subgroup motifs, dynamic discovered-factor recipes, and out-of-sample promotion.
- **Status:** completed

### Phase 10: Execute the Enterprise Blueprint
- [x] Phase 0 contract freeze and baseline.
- [x] Phase 1 correct single-event replay and workbench layout.
- [x] Phase 2 historical point-in-time primitive and event-aligned sequence materialization.
  - [x] Phase 3 deterministic sequence-pattern discovery, controls, outcomes, and regime/out-of-sample validation.
  - [x] Phase 4 factor-set typed expression, immutable version lifecycle, promoted recipe screen preview, strategy evaluator integration, lineage, watchlist chain, and health extensions are implemented.
- [~] Phase 5 full research workbench. (job creation, polling and discovery evidence are implemented; replay, sequence, detail and factor-set editing panes remain)
- [~] Phase 6 governed AI and strict Agent tools. (three strict read tools and governed deployment complete; study-synthesis capability remains)
- [~] Phase 7 performance, backfill, quality, and Release verification. (full Release recreation and health/governance checks complete; performance/backfill dashboards and end-to-end acceptance remain)
- **Status:** in progress

## Decisions Made

| Decision | Rationale |
| --- | --- |
| Treat an observed limit-up/large-move pattern as evidence, not a causal conclusion. | Cross-sectional coincidence alone cannot establish a stock's reason for moving. |
| Use a deterministic bounded evidence snapshot before any AI explanation. | The model may summarize and prioritize supplied evidence, but may not invent event causes or screening rules. |
| Keep factor-derived screening explicit and editable. | Reuse only typed Screener criteria and approved strategies; never execute model-generated SQL or opaque factors. |
| Expose the new factual read through one bounded read-only Agent tool. | It is reusable market evidence and can safely enforce date, universe, and result-size bounds. |
| Model factor evidence by time stage and independent family. | Intraday industry behaviour, pre-event technical structure, nine MSI factors, margin, breadth, and post-event returns must not be collapsed into one opaque score. |
| Keep the first release deterministic. | The user-facing result needs source/date/coverage-backed facts first; adding AI synthesis now would require a separate governed capability rather than a UI-only model call. |
| Make AI synthesis explicit and evidence-referenced. | The model receives only deterministic evidence references and must return structured hypotheses with evidence and counter-evidence references; it cannot supply investment advice or causal claims. |
| Treat market/industry-cycle factors as plug-ins. | The same observatory must support future swing factors and new event types without a breaking API or a rewritten UI. |
| Treat familiar indicators as primitive observations, not the discovery vocabulary. | Recurring and special factors must be learned from event-aligned raw sequences; duration, change points, ranges, and subgroup conditions come from samples and controls rather than fixed MA/volume rules. |

## Errors Encountered

| Error | Resolution |
| --- | --- |
| The PowerShell planning helper targets root-level files and found an unrelated prior plan. | Created this isolated plan directory under `.planning`; do not modify root planning files. |
| The direct observatory endpoint initially returned 502 while Release services were still registering. | Retried after startup; the Market service registered normally and the gateway returned the complete response with HTTP 200. |
| The configured OpenAI-compatible provider initially returned HTTP 502 for the new AI route. | The governed error path worked and a later runtime check succeeded; provider availability is still external state and is not used as the deterministic research completion gate. |
