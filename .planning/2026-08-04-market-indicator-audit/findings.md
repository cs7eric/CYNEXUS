# Findings: Market Indicator Read-Only Audit

## Scope

- CodeGraph index contains 2,910 files and 49,157 symbols; it reports one pending modified file.
- The installed CLI exposes `query`, `callers`, `callees`, and `impact`; it does not expose a `search` command.

## Initial Module Map

- The feature spans four layers: Python data calculation/persistence, Java query API/domain, React service/page, and an autonomous Agent indicator tool.
- Primary Python files are `domain/market/market_indicator.py`, `pipelines/market/market_indicator_refresh.py`, `storage/postgres/market_indicator.py`, the TDX indicator source, and a registered refresh job.
- Java exposes `GET /api/market/indicator/latest` and `GET /api/market/indicator/industry/detail` through controller -> app service -> domain query service -> repository -> MyBatis mapper.
- The UI has a dedicated market-indicator page plus market-overview consumption.
- Existing tests cover Python domain/source/storage and Agent dispatch, but discovery has not yet established whether they validate real usefulness or only mechanics.

## Product And Usage Context

- The UI product goal is a dense A-share research/trading workbench where data freshness, market state, and availability are explicit.
- No dedicated durable design or data-contract document was found for `market-indicator`; only the implementation files and the broader `market_overview` documents exist. This is an early signal that expected behavior and success criteria may never have been specified precisely.
- `fetchMarketIndicators` has only two UI consumers: the dedicated Market Indicator page and Market Overview.
- The event detector and TDX snapshot source each have only the refresh pipeline and a unit test as callers; there is no alternate production path or fallback producer.
- The core Python domain and storage files are comparatively large, while source/job wrappers are small; the feature's usefulness therefore depends heavily on the calculations and persisted query shape rather than orchestration complexity.

## Python Producer Findings

- The implemented adaptive candidate selector (`select_indicator_candidates`) is dead production code. The refresh pipeline never calls it; it is exercised only by a unit test.
- Production instead restricts the universe to stocks that appeared in `market_limit_daily_stocks` as limit-up or limit-down within the last 365 days. Every candidate is labeled `historical_limit`; there is no hot/rotation/coverage behavior despite those concepts existing in domain code.
- Consequently, “market breadth” and per-industry advancing/declining counts are computed only over this filtered historical-limit universe, not the A-share market or even a stable industry universe. The numbers cannot represent the labels shown to users.
- The pipeline has one live source (`eltdx.quotes`) and no fallback, freshness check, trading-session guard, completeness threshold, or quality gate. An empty quote response still returns `success: true` with zero rows.
- Limit states are inferred only from percentage change bands. `limit_up_touch` means merely “within 0.5 percentage points of the threshold,” not that the price actually touched the exchange limit; `sealed/opened` similarly do not use actual limit prices, order-book state, or intraday high.
- Rapid-rise/fall events compare the current quote with only the latest persisted quote for the day. Their meaning therefore depends entirely on scheduler cadence and missed runs, yet the model has no explicit time window or rate normalization.
- Industry snapshots count only current fetched rows. Missing quotes are silently dropped, so breadth denominators drift between refreshes and can create misleading cross-time comparisons.
- The source normalization expects several fields (`turnover_rate`, actual limit prices) that the tested source contract does not demonstrate. Tests validate only a zero-price placeholder and do not assert real eltdx field mapping.
- Persistence tests cover only stable snapshot IDs. There are no pipeline integration tests for the historical-limit filter, empty-source behavior, event deduplication, industry completeness, or query-facing semantics.

## Scheduler And Declared Intent

- The scheduler runs every three minutes during 09:30–11:30 and 13:00–15:05 on workdays.
- Durable descriptions explicitly define the producer as a monitor for stocks that hit a limit-up/down within the previous 365 calendar days, not a whole-market indicator engine.
- The pipeline README first calls it an “active stock universe,” then clarifies the historical-limit restriction. This vocabulary is easy to misread and does not match UI concepts such as market breadth.
- `rapid_change_pct=3` therefore means a three-percentage-point change between adjacent scheduler captures (nominally three minutes), but the event object does not record the elapsed interval or distinguish delayed/missed captures.

## Java Query And SQL Findings

- All serving queries are anchored to the latest `market_indicator_candidate_pool.captured_at`, not the latest successful snapshot capture.
- The pipeline writes the candidate pool before saving snapshots. If eltdx returns zero rows, the pipeline still persists a newer candidate capture and reports success; Java then joins against that empty capture and hides the previous valid data. One transient empty run can therefore make the whole page appear empty until a later successful run.
- “First limit-up” and “first event” industry queries are restricted to the latest capture. They do not search earlier captures in the trading day, so these fields reset every three minutes and are not actually the day's first event.
- The API performs separate queries for snapshots, limit-up, limit-down, events, breadth, and industries. There is no single resolved dataset version/capture contract returned across them.
- `snapshotCount` is the size of the limited snapshot list (default 100), not the actual captured universe size. It can understate the dataset while breadth reports a different total.
- Industry detail and relation calculations operate only on currently fetched historical-limit candidates, not full industry constituents; nevertheless the DTO names do not expose that sampling scope.
- The industry-relation SQL sets `overlap_count` equal to the target industry's current sampled stock count. It never calculates stock overlap with the selected source industry. The downstream “relation score” is therefore based on a mislabeled/nonexistent overlap signal.
- Because THS membership is selected as one industry per stock, true stock overlap between the source and target THS industries is not even represented by the current query model; relation results are essentially shared TDX56 mapping plus direction, presented as stronger quantitative evidence than exists.

## Runtime Context

- Shared infrastructure and the Release application stack are online; no application was started or restarted for this audit.
- Running versions are not uniform: market/UI are `1.1.398-260804`, while core scheduler is `1.1.397-260801` and several other services are older. This can matter because the scheduler is the producer trigger owner.
- PostgreSQL contains the market-indicator tables and an enabled three-minute scheduler definition with the expected 365-day historical-limit defaults.
- The latest run succeeded at 15:01 China time, writing 1,767 snapshots from a 1,783-code candidate pool; the job has 339 total runs and 4 recorded failures.
- The active stock master contains 5,530 stocks. The module monitors 1,783 historical-limit candidates, about 32% of the active market, yet the UI derives a “market breadth” verdict from this subset.
- All 1,767 latest snapshots have `turnover_rate`, `limit_up_price`, and `limit_down_price` as NULL. The UI nevertheless renders turnover and board-status concepts as if the data existed.
- The actual eltdx payload contains order-book levels and intraday high/low, but the detector ignores them and derives touch/seal/open solely from percentage bands.
- At the latest capture the module classified 135 stocks as sealed limit-up. The daily limit table has no row for the current trading date yet, so there is no same-day reconciliation or truth-source validation in the pipeline.
- Of 88 latest industry rows, only one had a “first event,” and its timestamp exactly equals the latest capture time. This confirms the UI's “最早信号” is the latest tick's event, not the day's earliest signal.
- A real poisoned capture exists at 2026-07-28 21:14 China time: 800 candidate rows were committed, snapshot persistence then failed with a primary-key collision, leaving zero snapshots for that capture. This is direct evidence that the candidate-anchor design can blank the serving API after a partial failure.
- Other recorded failures include total eltdx host connection failure and a pipeline code error. The feature has no fallback source, so provider availability directly determines usefulness.
- The current API is healthy, but with `limit=5` it reports `snapshotCount=5` while breadth reports 1,767, proving `snapshotCount` is a response-page count rather than data coverage.
- The limit history truth table's latest trade date is 2026-08-03, not the current 2026-08-04 session, so the live board classification cannot be reconciled against it intraday.
- The dedicated page requests `limit=2500`. The current live response is about 1.06 MB and took roughly 734 ms locally; the page repeats this request every 60 seconds even though production data refreshes every three minutes.
- The first capture of the day generated 217 of 296 rapid-rise events (73%) and 25 of 42 rapid-fall events (60%). This follows directly from comparing the first observed daily change against a synthetic previous value of zero, so most “rapid” events are opening/late-start baseline artifacts rather than measured three-minute acceleration.

## UI Semantics And Workflow Findings

- The page offers a tab named “全市场” while explicitly using only the 365-day historical-limit pool. That contradiction makes the main interaction promise false even though a smaller explanatory sentence exists.
- “市场广度 / 赚钱效应扩散 / 多头占优” is calculated from the biased 32% pool. This is the most consequential product-level mismatch: the UI turns a high-volatility watchlist into a market regime signal.
- The UI's “快速变化” filter uses absolute daily change `abs(changePct) >= 3`, while backend rapid events mean a three-percentage-point change between captures. The table and event feed therefore describe different phenomena with the same words.
- “行情连接正常” is based only on whether any data exists. It does not check age, trading date, scheduler health, source failures, or capture completeness; refresh errors preserve old payloads, which can continue to look healthy.
- “涨停封板/触板/开板” and “最早信号” labels overstate the data. The former are percentage-band guesses; the latter resets to the newest capture.
- Industry “成分股” totals and strength bars are sampled historical-limit members, not full industry constituents. The UI does not expose this denominator limitation at the point of interpretation.
- The relation panel presents a `/100` score and “映射关联 N 只,” but the SQL does not calculate source-target overlap. The numeric confidence is visually precise without valid underlying evidence.
- The Market Overview page fetches indicator data mainly to display up to four recent events. Its core verdict and breadth calculations come from other market-history data. Thus this module has little influence on the main decision workflow despite its cost and complexity.
- No dedicated UI tests or Java market-indicator tests were found. Existing Java tests only verify Agent-tool dispatch contracts, not query semantics or user-facing correctness.

## Development State

- The market-indicator implementation across Python, Java, UI, SQL, and Agent handler is currently untracked in Git. It is deployed and running, but it has not reached a committed baseline in this worktree.
- Three earlier planning directories exist for the feature (`market-indicator`, `market-indicator-redesign`, and `market-indicator-v2`), indicating repeated redesign attempts within a short period. Their contents are being treated as historical design evidence, not execution instructions.
- Final CodeGraph status still reports one pending modified source file. Exact live source files were read directly for all critical findings, so the report does not rely on stale indexed snippets for those conclusions.

## Implementation Scope For Fix

- User explicitly confirmed the historical limit-up/limit-down pool is intentional and must remain; the fix must not expand collection to all active stocks.
- The current raw eltdx quote payload contains structured `buy_levels` and `sell_levels` arrays with price/volume, so limit sealing can be made evidence-based without adding a new upstream provider.
- The existing `sec_stock_basic.float_shares` column is available in Release PostgreSQL, so turnover rate can be derived from `total_hand` when the float-share value is present.

## Intent Drift

- The original plan explicitly targeted full-market latest-per-code breadth, retained first events across captures, and researched rich eltdx capabilities such as `shortline_indicators`, depth, push polling, seal ratios, and ladder level.
- A later reliability task replaced full-market three-minute polling with an 800-code hot/rotation candidate pool. The current implementation then replaced that again with all stocks having any limit-up/down during the last year (~1,783 codes), leaving the tiered selector as dead code.
- The UI redesign was built around “市场状态 -> 行业 -> 个股异动” and deliberately assumed the backend contract was already semantically suitable. The visual layer improved the presentation but did not challenge the denominator or signal validity.
- The V2 design promised full-market coverage and explainable relation scoring. Current runtime coverage is only 32%, and the relation SQL does not calculate the promised overlap.
- No production code uses the originally researched advanced eltdx indicator/depth/push APIs. The feature ultimately uses only batch quotes and simple percentage comparisons, which explains why it feels much weaker than its intended “盘中研判” positioning.
- This is not one isolated bug. It is accumulated design drift: product labels and UI were preserved while the producer universe and signal model were repeatedly simplified for reliability/performance.

## Ranked Remediation Direction

1. Split “whole-market state” from “intraday anomaly radar.” Whole-market breadth requires a stable/full denominator; anomaly detection may use a dynamic bounded pool. They must not share labels or counts as if they were the same dataset.
2. Make the serving version transactional and quality-gated: publish only a successful capture with minimum completeness/freshness, and keep the last good version on partial failure.
3. Redefine signals from observable market facts: actual price-limit calculation, intraday high touch, order-book seal/open state, elapsed-time-normalized acceleration, amount/volume acceleration, and relative strength versus industry/index.
4. Establish one signal contract used by backend events, UI tabs, breadth counts, and Agent tools. “Rapid” must have one definition and include window, baseline, severity, and evidence.
5. Rebuild daily sequencing correctly: compute first touch/seal/open and first anomaly over all captures in the trading day, with restart/backfill semantics.
6. Remove or redesign the current industry relation score until a valid relation graph and overlap/lead-lag metric exist. Mapping affinity and market co-movement should be shown as separate evidence.
7. Add actionability: ranked events, novelty/persistence, false-positive suppression, stock-chart/watchlist/analysis actions, and replay/evaluation against later returns.
8. Reduce payload and polling cost through summary/detail endpoints, pagination/deltas, and refresh cadence aligned with the producer.
9. Add semantic integration tests and live data-quality checks for universe coverage, source fields, partial captures, event timing, limit-board truth, stale status, and API denominator consistency.
