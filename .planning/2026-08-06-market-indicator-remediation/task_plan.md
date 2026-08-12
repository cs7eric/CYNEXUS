# Task Plan: Market Indicator Remediation

## Objective

Implement the `/market/indicator` remediation while restoring the intended distinction between intraday discovery, full-market confirmation, and post-market replay.

## Phases

- [x] Phase 1: Audit current UI, API, Python calculation, Redis scope, and live response behavior.
- [x] Phase 2: Reconcile the audit with the prior Market Indicator product redesign proposal.
- [x] Phase 3: Define the target contracts, calculation rules, API boundaries, UI information architecture, migration path, and acceptance tests.
- [completed] Phase 4: Correct data scope/metadata contracts and deterministic calculation defects.
- [completed] Phase 5: Rebuild the first UI information-architecture layer around radar/review modes, explicit scope, pool/full tabs, and evidence wording.
- [completed] Phase 6: Add focused tests, compile/build, and verify the live route/API.
- [ ] Phase 7: Rebuild Market Indicator as an event-anchored intraday radar and 1m post-market replay workbench.

## Phase 7: Event-Anchored Replay Workbench

### Product outcome

The page must answer four questions for every material move: **what moved first, what market context it occurred in, how the sector spread across members, and whether the move held or failed**. A sector rank without the first-limit-up/event anchor and subsequent outcome is not a transmission conclusion.

### 7.1 Evidence model and definitions

Create one deterministic evidence chain per event window:

```text
MarketContext1m -> AnomalyEvent -> LimitAnchor -> SectorEpisode -> Outcome -> AIExplanation
```

- `MarketContext1m`: index 1m bars (000001/399001/399006), market breadth, turnover, limit-up/down counts, break-board count, sentiment factors, and sector breadth at the same minute. Every record carries `tradeDate`, `asOf`, source, freshness, and coverage.
- `AnomalyEvent`: market-wide or stock-level event with `eventAt`, direction, trigger metric, baseline window, affected count, and evidence IDs. Keep `rapid_rise`/`rapid_fall` for intraday discovery labels, but expose user-facing labels as `market acceleration`, `market deterioration`, `sector breadth expansion`, or `sector breadth contraction` according to the calculated context.
- `LimitAnchor`: separate concepts and fields: `firstLimitUpAt` (first observed touch), `firstEffectiveSealAt` (first seal that remains sealed for the configured confirmation window), `firstIndustryLimitUpAt`, `streak`, `breakCount`, `reopenCount`, `reason`, and `anchorQuality`. “当日首板”, “连板高度”, and “行业首个涨停” must never be conflated.
- `SectorEpisode`: one lifecycle per `(tradeDate, industryCode, direction, startAt)`, built by merging adjacent captures within a 3-minute gap. Store leader, first-limit anchor, member waves, market-relative and sector-relative returns, coverage, and invalidation conditions. Do not persist every capture as an independent episode.
- `Outcome`: calculate forward returns and breadth at `+5m/+15m/+30m/close` from 1m bars, plus seal persistence, break/reseal, and market-relative excess return. Mark `effective`, `failed`, `mixed`, or `insufficient_data`; never infer outcome from the final snapshot alone.

Minimum calculation rules:

1. Normalize all event times to Asia/Shanghai trading minutes; exclude lunch break from elapsed-minute calculations.
2. Require at least 3 valid 1m observations before an event and at least 80% minute coverage in the evaluated forward window. Missing bars remain explicit gaps.
3. Event trigger uses a configurable 5-minute return/volume z-score against the prior 30 valid minutes, combined with breadth delta; a single daily percent jump is not an intraday event.
4. Sector transmission requires (a) an anchor member, (b) at least two additional members responding within 15 trading minutes, and (c) sector excess return above the market benchmark. Otherwise classify as observation only.
5. Effective seal requires the price to remain at/above the limit threshold for 3 consecutive valid minutes or a documented close-and-reseal sequence; one touch is not a sealed board.
6. Never use close/after-event data in a pre-event or intraday `asOf` response. Post-market replay may use the full day only after the selected replay timestamp.

### 7.2 Intraday radar information architecture

Replace the current metric-heavy right panel with a working order:

1. **Market state strip**: index change and 1m slope, breadth (`up/down/flat`), limit-up/limit-down/break-board, turnover pace, sentiment level, and a plain-language state (`risk-on`, `risk-off`, `rotation`, `range`). Show `asOf`, source, and coverage beside the state.
2. **Event feed**: chronological market anomalies and limit anchors. Each row shows time, event kind, lead stock, industry, affected count, 5m return, breadth delta, and an evidence badge. Selecting a row drives every lower panel.
3. **First-board ladder**: tabs for `首板`, `连板`, `炸板/回封`, `跌停/开板`. Sort by effective seal time, then streak and seal amount when available. Include industry, first-limit time, response count in 15m, and current seal state.
4. **Sector response board**: rank by a declared score, not a vague “transmission” label. Display leader -> wave 1 -> wave 2, lag, response coverage, excess return, and invalidation. Rows without an anchor are labelled `横截面观察`.
5. **Evidence drawer**: show the synchronized index/sector/member 1m mini-chart with vertical event markers, the exact metric values used, missing-data warnings, and the outcome checkpoints. This is the default explanation surface; AI is secondary.

Radar refresh policy: poll only the compact state/event endpoints at the effective backend cadence (minimum 15s); fetch heavy timeline/member evidence on selection. Keep payloads bounded (feed 50, ladder 100, sector board 30).

### 7.3 Post-market replay information architecture

Add an explicit `盘后复盘` workspace rather than reusing the intraday snapshot:

1. Header controls: trading date, replay cursor (`09:30` to `15:00`), play/pause, step 1m/5m, and `只看首板/只看大盘异动/只看失败` filters. Cursor is an `asOf` boundary.
2. **Replay timeline**: one shared x-axis with index bars, market breadth, limit ladder marks, market anomalies, and sector episode starts. Clicking a marker sets the cursor and selected episode.
3. **Sector propagation lanes**: selected industry plotted as leader, first responders, second responders, and non-responders; annotate `+5/+15/+30/close` outcomes and break/reseal points. Provide a reason code for failure: `无跟随`, `大盘逆风`, `冲高回落`, `封板不稳`, `数据不足`.
4. **Day scoreboard**: top effective episodes, failed episodes, first-board industries, strongest/weakest market regimes, and coverage/data gaps. This is the summary users can act on for research, not a list of raw metrics.
5. **AI review**: explicit `生成复盘解释` action for the selected event/day. AI receives only the computed evidence packet and returns structured `summary`, `drivers`, `counterEvidence`, `failureConditions`, `uncertainties`, and `evidenceRefs`. It cannot change scores, invent prices, or issue buy/sell instructions.

### 7.4 API and persistence boundary

Prefer compact, composable endpoints:

```text
GET  /api/market/indicator/radar/state?tradeDate=&asOf=
GET  /api/market/indicator/radar/events?tradeDate=&asOf=&kind=&limit=
GET  /api/market/indicator/radar/limit-ladder?tradeDate=&asOf=&board=&limit=
GET  /api/market/indicator/replay/summary?tradeDate=
GET  /api/market/indicator/replay/timeline?tradeDate=&from=&to=&industryCode=&eventId=
GET  /api/market/indicator/replay/episode/{episodeId}
POST /api/market/indicator/replay/explain
```

Persistence additions should be append-only and versioned:

- `market_indicator_minute_context` keyed by `(trade_date, minute)` with market/index/breadth/sentiment/coverage fields.
- `market_indicator_limit_anchors` keyed by `(trade_date, code, anchor_type)` with touch/seal/break/reseal timestamps and source.
- `market_indicator_sector_episodes` keyed by episode ID with lifecycle fields, leader/waves JSON, score components, and algorithm version.
- `market_indicator_sector_outcomes` keyed by episode ID and horizon (`5m`, `15m`, `30m`, `close`).
- `market_indicator_ai_reviews` keyed by evidence hash + prompt version; cache only completed structured output and retain execution metadata.

Keep existing snapshot/event tables as raw evidence and backfill sources. Do not make the UI join raw tables or payload JSON directly.

### 7.5 Score and explainability contract

Expose components alongside the rank:

```text
episodeScore =
  0.25 * anchorQuality
  + 0.20 * responseCoverage
  + 0.15 * responseSpeed
  + 0.15 * sectorExcessReturn
  + 0.10 * breadthExpansion
  + 0.10 * sealPersistence
  - 0.15 * marketHeadwind
  - 0.10 * dataPenalty
```

Weights are configuration, versioned with the algorithm. The UI must show the top positive and negative contributors and a “why this is ranked here” sentence generated from those components, not from AI.

### 7.6 AI capability requirements

If implemented, use stable capability `market_indicator_replay_explanation` across Java, `ai_prompt`, `ai_usage_bindings`, and `AI_CAPABILITIES`. Add an idempotent DB-managed prompt, provider binding, schema validation, bounded input, fallback text, execution log, and Provider UI routing entry. The prompt must explicitly forbid external facts, unsupported causality, confidence/score edits, and trading advice. Call only on user action and cache by evidence hash + prompt version.

### 7.7 Delivery phases and acceptance tests

- Phase A: minute context and limit-anchor query contracts; verify `asOf` truncation, trading-minute math, missing bars, and first-board definitions.
- Phase B: lifecycle episode aggregator and outcome calculator; replay fixtures must cover effective spread, failed spread, market headwind, break/reseal, and insufficient data.
- Phase C: radar/replay APIs and bounded payloads; contract tests verify event ordering and score-component provenance.
- Phase D: UI timeline, synchronized detail panes, keyboard stepping, loading/error/stale states, and responsive 3:7 layout.
- Phase E: optional AI explanation capability with prompt/binding/provider/validation/logging tests.

Acceptance examples:

- Selecting a 10:15 first effective seal shows the market state at 10:15, the first two response waves, exact +5/+15/+30/close returns, and any break/reseal markers.
- A sector with only one limit-up stock is never labelled transmission.
- Replaying at 10:20 cannot display a 10:35 event or close result.
- A failed episode states at least one measurable reason and links to the counter-evidence minute/stock.
- Every displayed number has date, minute/asOf, source, and coverage; missing 1m bars are visible.
- AI output is unavailable/stale-safe, evidence-referenced, and never changes deterministic ranking.

## Verification Notes

- Release `market` and `ui` rebuilt/recreated as `1.1.398-260806-RELEASE`.
- Live latest/transmission endpoints return the new scope, coverage, cadence, and benchmark metadata.
- Current capture has no confirmed transmission episodes; the UI now presents that as no confirmation, not as a causal sector conclusion.
- Targeted ESLint retains pre-existing React hook/compiler findings in `market-indicator/index.tsx`; production build and typecheck pass.

## Scope Boundaries

- Avoid unrelated files and preserve existing worktree changes.
- Preserve the existing dual-speed collection decision: priority discovery and full-market confirmation remain distinct evidence layers.
- Treat historic limit-up/down coverage as a discovery cohort, never as implicit full-market sector confirmation.

## Phase 7 implementation status

- [x] Composable radar/replay endpoints and bounded Agent read operations.
- [x] Replay scoreboard, Episode selection/detail, coverage warning, and deterministic evidence explanation UI.
- [x] Release deployment and live `ready`/`pending` verification.
- [ ] Dedicated minute-context/limit-anchor/outcome persistence and true provider-backed `market_indicator_replay_explanation` capability remain future work. The current UI labels the explanation as deterministic fallback and does not represent it as AI output.
