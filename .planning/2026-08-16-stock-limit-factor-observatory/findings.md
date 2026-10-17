# Findings and Decisions

## User Requirements

- Add a Screener tab for observing limit-up and large-move stocks.
- Review price action before the move alongside market sentiment, industry state and capital, style, technical trend, recent path, margin, futures, intraday, and daily K-line evidence where available.
- Analyse intraday industry behaviour around an event, not only end-of-day industry state.
- Use the existing nine MSI market-sentiment factors as separate inputs and preserve their source dates, coverage, and direction.
- Consider market regime, industry cycle, margin, breadth, new-low/new-high distributions, and the relevant index path as independent evidence; future swing-factor and event models must fit the same structure.
- Identify recurring multi-factor evidence without claiming causality from partial observations.
- Reuse approved factor conditions for deterministic stock screening, observation, and backtesting.
- Expose new factual capabilities to the autonomous Agent and use AI only as a grounded, opt-in synthesis layer.

## Initial Constraints

- Preserve the existing deterministic Screener and its editable typed criteria.
- An AI capability requires the full prompt, provider, binding, migration, UI routing, validation, and logging contract.
- Agent reads must be compact, date bounded, read-only, schema validated, and source/freshness labeled.
- Existing worktree changes are user-owned and must not be reset or rewritten.

## Research Findings

- `MarketPulseQueryDomainService.limitEmotion` already exposes a bounded daily limit-up, limit-down, and broken-board pool from `market_limit_daily_stocks`; each record includes the trade date, code, name, category, streak, change, and limit prices.
- The data pipeline materializes daily market-state context and attaches as-of-date industry factors. Market state also exposes breadth, limit-up/down count, industry leadership, and intraday capture context.
- The current Screener has a typed deterministic criteria contract and strategy/watchlist/backtest workflow. AI intent planning is already separated from deterministic screening.
- Existing market sources include daily/one-minute K-line, industry/style/concept membership, market sentiment, industry fund flow, margin, futures-position, and limit-up fund-flow archival data; each source needs an explicit coverage label rather than an assumed value.
- The current Pulse API already has `/api/market/pulse/limit-emotion?date=...`, with bounded per-day limit-up, limit-down, and broken-board pools, but it does not provide multi-day event history or per-stock factor evidence.
- `StockScreenerRepository` intentionally returns a bounded as-of daily snapshot and typed classification options. Its autonomous Agent tool is a thin bounded Feign adapter, which is the correct pattern for the new factual factor-observatory tool.
- Current limit-pool records are event metadata only. The new domain contract must join verified as-of data and return factor availability/coverage instead of filling missing margin, futures, or intraday values with inferred claims.
- The Market indicator domain already supplies deterministic market-state classes (`risk-on`, `risk-off`, `range`, `rotation`), breadth, radar events, industry snapshots, limit-ladder states, and replay/industry-transmission evidence. It explicitly reports partial/unavailable minute coverage, which must be retained in the observatory result.
- The Screener DTO already carries price trend, returns, liquidity, valuation, classification memberships, fund flow, and a compact price-volume-pattern metric group. These are suitable deterministic inputs for a factor evidence model, but do not include causal labels.
- The Market Research Feign contract is the module-safe integration boundary for a new Agent handler. The new endpoint should be appended there rather than granting the AI module repository access.
- `MsiIndexDailyBO` has exactly nine independently persisted components: `vol`, `turnover`, `priceStrength`, `riskAppetite`, `breadth`, `limitEmotion`, `profitEffect`, `sectorBreadth`, and `styleRisk`; the existing domain service also exposes their explicit weights. The observatory will return all nine values rather than only the composite score.
- `MarketIndicatorQueryDomainService` already classifies market state from breadth, exposes industry snapshots, radar/limit-ladder events, replay timelines, and a Trade Review materialization whose `dataStatus` and missing reasons must be propagated unchanged. This is the source for intraday industry and market evidence.
- Margin data has bounded market, industry, and stock reads. The first observatory domain read can take a bounded date-aligned margin stock slice and match it with the selected cohort; no direct mapper or application-service access is needed.
- The existing screener row already contains returns over 5/10/30/60/252 days, fund flow, TDX/THS industry, concept/style memberships, turnover, volume ratio, and daily technical/candlestick measurements. Requesting the pre-event snapshot at or before `eventDate - 1 day` avoids future leakage.
- `StockScreenerRepository.findSnapshotOnOrBefore` already produces a full A-share as-of snapshot joined with stock master data, fund flow, THS/TDX classifications, concept/style memberships, and derived daily price-volume metrics. It can supply the core per-event evidence without a new storage job.
- The observatory should call the Screener snapshot with a broad technical-metric request, match persisted limit-pool codes against that snapshot, and report any row with unavailable metrics as incomplete rather than excluding it silently.
- The current Market indicator domain has deterministic breadth/market-state classification, limit ladder, industry snapshots, and replay evidence. Its repository can provide the market and industry evidence for the same date, avoiding client-side queries and multiple-date drift.
- The completed AI capability is `market_event_factor_analysis`. It uses a DB-managed `v1` prompt and enabled usage binding, an explicit provider/model selector, structured output validation, and bounded execution-log previews.
- AI synthesis is constrained to the supplied deterministic evidence references. Each hypothesis is `COHORT` or `EVENT`; event hypotheses must use a trusted event code and all evidence/counter-evidence references must be present in the supplied envelope. The prompt prohibits external facts, causal assertions, hidden reasoning, prices, positions, and trading advice.
- `LIMIT_UP` cohorts can safely avoid the expensive event-day Screener snapshot because their event population is persisted in limit-emotion data. Their pre-event evidence comes from the prior available Screener snapshot. `LARGE_MOVE` cannot use this optimization yet because the event-day Screener snapshot defines that cohort.
- The deployed `openai_compatible` route initially failed upstream with HTTP 502, then succeeded in a later runtime check. The latest observed AI request took roughly 105 seconds end to end (about 90 seconds provider latency), so availability and latency remain separate operational concerns; neither may block deterministic research results.

## Technical Decisions

| Decision | Rationale |
| --- | --- |
| Use the existing daily limit pool as the event universe. | It is persisted, date-addressable, and includes confirmed limit-up/broken-board status and streak rather than inferring events only from a generic percentage threshold. |
| Keep a separate `LARGE_MOVE` cohort only where the daily Screener snapshot can verify the threshold. | This avoids treating different board limit rules as a universal percentage event. |
| Build a compact cross-sectional factor profile first, then allow optional AI synthesis. | Market state and individual factors must remain inspectable when an AI provider is unavailable or declines to make an unsupported causal claim. |
| Make the first API date-scoped and cohort-scoped, not an unbounded historical data export. | A single selected daily pool can be replayed safely and supports a bounded sample for factor coverage/statistics. Historical comparison can be added with explicit windows later. |
| Use a stable factor envelope: `event`, `pre_event`, `intraday`, `market_regime`, `industry_regime`, `cross_market`, `post_event`, and `coverage`. | Every new factor family has a clear temporal meaning and can return unavailable/counter-evidence without changing consumers. |
| Return independent factor measurements plus a transparent profile summary, never a universal "limit-up reason" score. | A high-level score would hide contradictory evidence and encourage unsupported causal narratives. |
| Defer model-backed synthesis from the first release. | It would need a separately governed AI capability, prompt seed, provider/model binding, structured-output validation, logging, migrations, and an explicit opt-in UI route. The factual evidence endpoint and Agent tool are complete without it. |

## Issues Encountered

| Issue | Resolution |
| --- | --- |
| Root planning files belong to a different completed task. | Use this isolated `.planning` scope. |
| The first date-scoped observatory request takes about 26 seconds against the current full daily snapshot implementation. | The response is bounded and correct, but snapshot materialization/caching is the next performance improvement before wider historical cohort analysis. |

## 2026-08-16 Implementation-plan Reassessment

- The requested deliverable is now a directly executable enterprise implementation plan, not another incremental UI patch.
- The existing observatory, governed AI route, read-only Agent tool, strategy/backtest/watchlist infrastructure, and time-stage envelope are reusable foundations.
- The missing center is a persistent research domain: versioned event cohorts, point-in-time feature snapshots, controls, outcome labels, factor discovery/validation, factor-set lineage, and tracked deployment results.
- The implementation plan must separate deterministic research computation from AI synthesis, preserve DDD boundaries, and include migrations, materialization jobs, APIs, Agent governance, UI states, performance, release, and measurable acceptance gates.
- Java must own research-domain policy, lifecycle, API orchestration, permissions, AI routing, and UI response contracts; Python may materialize market-data feature snapshots/outcomes only through dry-run-capable pipelines and JobRunner jobs.
- New stable factual reads should be exposed as bounded task-level Agent tools through Java contracts; factor-set, strategy, backtest, and watchlist mutations require the existing preview-confirm-audit path rather than autonomous write handlers.
- Release completion requires idempotent migrations, synchronized Maven/UI snapshot versions, rebuilt versioned Release images, and runtime verification; compilation alone is not an acceptance gate.
- The current observatory contract already carries per-event `factors`, stage/source/as-of/status, concepts/styles, and market factor families, but the UI only renders `marketFactors`; the new workbench can preserve the envelope while replacing `screeningHints` with validated, versioned factor-study outputs.
- Existing strategy versions, candidate evidence, asynchronous backtests, named watchlists, entry-price snapshots, performance history, and strategy health are concrete reuse points. They need additive research lineage (`studyId`, `factorSetVersionId`, validation snapshot, source event set), not a parallel duplicate strategy/watchlist subsystem.
- Current strategy backtests already support lookback, signal interval, holding period, candidate cap, transaction cost, benchmark excess return, and target symbols; event-factor validation must either reuse this engine through a typed factor-set evaluator or extend it without embedding research rules in controllers/AppServices.
- The project already materializes the exact intraday evidence needed for Phase 1: `trade_review_security_daily_fact`, indicator events, limit-state intervals, market minute state, THS/TDX industry minute state, minute pools, industry daily state, daily pools, and trend phases. The current observatory only reads the manifest status, so the first implementation should expose these existing typed facts before designing another minute-data mart.
- `MarketIndicatorQueryDomainService` already supports full-day replay, industry filtering, market-state-as-of, transmission episodes/outcomes, and data-quality degradation. The new event replay API should compose these domain methods and stock minute bars; it should not query ClickHouse directly from the UI or AI module.
- Existing PostgreSQL strategy/watchlist/backtest tables are tenant/owner scoped and versioned. New research tables should reference strategy versions and candidates additively, preserve owner scope, and avoid replacing those mature execution tables.
- The current AI capability is correctly evidence-bounded but only flattens the single-day observatory into string references and samples at most 20 events. The implementation plan should keep this capability for case explanation, and add a separate research-synthesis capability that consumes persisted statistical study results rather than raw event rows.
- The current Agent output schema weakens `marketFactors`, `events`, and `screeningHints` to untyped objects. The plan must version the existing tool contract or replace it with strict event-replay and factor-study read tools, including freshness, source, coverage, sample-size, control, outcome, and truncation fields.

## 2026-08-16 Factor-discovery Correction

- The user's primary requirement is not to test a fixed catalog of familiar indicators. The system must discover recurring or subgroup-specific temporal structures from event-aligned raw sequences, then turn validated discoveries into versioned factors.
- Existing MA, return, volume-ratio, MSI, industry, margin, and futures values are primitive observations/context only. They must not constrain the vocabulary of discovered factors or be presented as the discovery result.
- “Sustained depressed volume followed by a sudden volume burst” must be detected as an adaptive state transition: discover each sample's low-volume regime length and change point, measure the burst relative to that regime and the stock's own historical distribution, then learn cohort-supported ranges. It must not be reduced to exactly three contraction days plus exactly three-times volume.
- The research engine needs two registries: a stable `PrimitiveFeatureRegistry` for observable series and a dynamic, versioned `DiscoveredFactorDefinition` registry for learned motifs/rules. Only out-of-sample validated discoveries may be promoted to screenable factor-set components.
- Discovery must cover full-cohort common motifs, regime/industry subgroups, and rare high-effect special motifs, while retaining unmatched samples and counter-evidence. AI may name and explain deterministic discoveries but may not invent them.

## 2026-08-17 Continuation Assessment

- Phase 0–2 are implemented and deployed; Phase 3, 5, 6 and 7 have partial implementation. Phase 4 has backend Factor Set create/update/validate/promote APIs but does not yet provide the required typed editor, screen preview, or strategy/backtest/watchlist lineage.
- The current `FactorResearchTab` is rendered in the Screener's left criteria pane. It can queue and poll studies and display basic discovery summaries, but it lacks the six-view workbench detail surfaces and a right-side Factor Set editor required by the implementation plan.
- Factor Set version retrieval currently uses the latest version row as `activeVersion`; the UI must treat this as the editable latest version, not as proof that it has been promoted to an active strategy handoff.
- Discovery candidates previously carried only aggregate support counts, so the execution service could not calculate chronological train/validation effects. Candidates now persist deterministic event/control sample references; execution uses the date-disjoint split to calculate train and validation effect separately and persists both values/status in validation evidence. Monthly, Regime and outcome stability remain distinct gates and are not inferred from support counts.
