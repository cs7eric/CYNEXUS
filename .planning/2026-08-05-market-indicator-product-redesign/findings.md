# Findings: Market Indicator Product Redesign

## Initial Intent

- Primary jobs: intraday signal detection and post-market review.
- Core questions: which stock leads or suppresses a sector, which stock changes sector sentiment, and under what broad-market regime an anomaly occurs.

## Confirmed Follow-up Decisions

- The desired user-visible refresh target is 5 seconds, not 1 minute.
- Before fixing the production refresh interval, benchmark the shortest complete full-market workflow: provider fetch -> normalize -> calculate -> publish -> query -> UI receive/render.
- Attempt full-market 5-second collection first. Use measured p50/p95/p99 latency, quote coverage, provider failure rate, CPU/memory, Redis/PostgreSQL/ClickHouse write pressure, API payload, and browser cost to choose the stable cadence.
- The historical limit-up/down pool remains useful as a priority tier and fallback, but should not be the only discovery universe if full-market polling is viable.
- Post-market review should include a degree of buy/sell reference guidance. Guidance must be evidence-based, conditional, confidence-bounded, and explicitly non-automatic.

## Initial 5-Second Cadence Check

- CodeGraph is current and confirms the production Market Indicator collector still flows through `market_indicator_refresh -> fetch_indicator_snapshots`.
- The dedicated Market Indicator UI currently refreshes every 60 seconds. Repository-wide 5-second UI polling found in the scheduler/data-source operational pages is not evidence that Market Indicator itself has a 5-second end-to-end data cadence.
- The current data source fetches batched eltdx snapshots and the pipeline performs normalization, prior-snapshot lookup, event detection, industry aggregation, Redis staging/publish, periodic PostgreSQL flush, and Java/UI query. The complete chain must be timed; changing only the React interval to 5 seconds would repeatedly serve the same capture.
- Repository search crossed a local Redis AOF containing large capture payloads; future searches will exclude `deploy/local/redis/data` and other runtime data directories.

## Completed 5-Second Runtime Assessment

- Core's scheduler runtime scans due jobs with a default fixed delay of 30 seconds. Its interval policy currently recognizes `every_minutes`; a five-second cron would still be bounded by the global scan interval unless the scheduler itself changes.
- `market_indicator_refresh` is currently triggered synchronously by the Data Executor API. It is not in `ASYNC_TRIGGER_JOBS`, so a five-second recurring design cannot safely be implemented as repeated full HTTP job executions.
- Data Executor's persisted JobRunner lock defaults to a one-hour lease. A morning/afternoon long-running session needs an explicit renewable leader lease and heartbeat rather than relying on that default alone.
- The current indicator source creates an eltdx client, fetches the provider code list, intersects it with metadata, and loops batches sequentially. Full-market feasibility therefore depends on measured batch size, pool size, concurrency, and warm-connection behavior.
- The existing Tencent adapter already supports realtime quote batches with a default batch size of 500 and 220 ms inter-batch delay. It is a realistic benchmark/fallback candidate, but its full-market URL sizing, rate stability, quote freshness, and concurrency must be measured.
- EastMoney breadth is currently serially paginated and AkShare retries full calls; neither should be assumed to be the primary five-second quote source without benchmark evidence.
- The current Redis design stores one immutable full capture JSON bundle containing rows, events, industry rows, and candidate rows. This is useful for V1 atomicity but is unsuitable as the full-market five-second read model.
- Saving roughly 5,500 stocks every five seconds would create about 15.84 million quote rows per trading day. Full-market high-frequency facts belong in batch-written ClickHouse with retention/aggregation tiers; PostgreSQL should store business events, episodes, advice, versions, and audit records.
- The target runtime should be one scheduled intraday session with warm connections, fixed five-second buckets, in-memory rolling windows, renewable leadership, no overlapping capture cycles, and adaptive cadence. Redis publishes context and episode read models, not full-market rows.

## Repository Discovery

- CodeGraph index is current: 2,935 files, 49,698 nodes, and 101,996 edges.
- The current feature spans React views/services, Java market query APIs/domain/repositories, Python indicator detection/refresh/storage, Redis hot cache, PostgreSQL tables, and a read-only autonomous Agent tool.
- Existing UI sections include breadth, event feed, limit boards, indicator ranking, industry ranking, and an industry detail panel.
- Existing backend concepts include market breadth, indicator events, stock snapshots, industry snapshots, industry relations, and industry detail.
- Prior task records exist for the original module, two redesign iterations, and a recent audit; these need to be reconciled before proposing another redesign.

## Historical Design And Audit Evidence

- The module has already been visually reorganized around “market state -> structure -> stock monitor -> events,” and later around industry drill-down and relation clues. The remaining weakness is therefore not primarily spacing or card layout.
- The most recent audit found accumulated intent drift: product labels outgrew the actual producer semantics, advanced eltdx capabilities were researched but not integrated, and a watch-pool monitor was presented too much like a market-wide regime engine.
- The 2026-08-04 fix intentionally retained the recent historical limit-up/down stock pool, hardened capture quality, improved limit/order-book semantics, corrected daily sequencing/query counts, and aligned UI terminology with pool scope.
- Current product constraints therefore require two distinct evidence layers: a broad-market context source and a pool-scoped anomaly/leadership source. The latter cannot by itself answer “what was the whole market doing?”
- `fetchMarketIndicators` has only two frontend consumers: the dedicated page and Market Overview. The dedicated page remains the primary place for a redesigned workflow.
- Existing API concepts cover current snapshot state and same-industry drill-down, but there is no confirmed first-class contract yet for leader-to-sector transmission, market-regime-at-event, persistence/decay, or forward reaction.

## Current UI Assessment

- The page is a pool monitor, not a two-mode trading workflow. It has no explicit “盘中雷达 / 盘后复盘” mode, date replay, event timeline replay, or session summary.
- The first decision block is pool breadth. It does not show CSI/Shanghai/ChiNext context, index trend, full-market breadth, style regime, liquidity, or the market state at an event timestamp.
- Industry ranking currently sorts by limit-up concentration, rapid event counts, or internal up/down breadth. These describe sector state but do not identify a leader, follower, suppressor, transmission start time, propagation breadth, lag, persistence, or failure.
- Industry drill-down shows current constituent snapshots and mapping-based related sectors. It explicitly avoids causal claims, which is correct, but it does not answer the user's causal/temporal question about which stock moved first and whether peers followed.
- The stock monitor is snapshot-oriented: price, daily change, daily range, turnover, board status, and capture time. It lacks sector-relative return, index-relative return, short-window acceleration, volume/amount acceleration, contribution, follower count, confirmation score, and post-signal outcome.
- The event feed is a raw chronological state-transition list. Events are not clustered into episodes, ranked for actionability, linked to sector propagation, or enriched with market-regime context.
- Loading/empty/error semantics are reasonably covered, and the current visual system is appropriately restrained. The redesign should preserve the shell and visual language while replacing the information model.

## Current Data-Model Assessment

- Rapid events are thresholded changes in daily percentage return between adjacent captures (default 1 percentage point, max 10-minute gap). They are not normalized velocity, volume-confirmed moves, sector-relative moves, or leader/follower episodes.
- Industry snapshots aggregate current counts and the day's first event/limit event, but do not persist per-stock contribution, propagation sequence, follower lag, response breadth, peak response, decay, or failed transmission.
- Snapshot/API fields are limited to price/volume/turnover/limit state plus current industry. There are no market-context fields or benchmark-relative fields attached to events.
- The Java relation score is mapping confidence plus same/opposite current direction. It is a semantic mapping/co-movement clue, not temporal influence or causality.
- The serving SQL is latest-capture oriented, with recent daily events and current constituents. A genuine review experience will require time-indexed episode and outcome queries rather than simply reusing the latest overview payload.
- The historical limit-up/down pool remains a useful “high-attention universe,” but leadership should be evaluated against complete sector constituents when possible; otherwise the UI must explicitly label results as “池内传播” rather than sector-wide influence.

## Competitor Patterns (Public Product Material, 2026-08-05)

- 开盘啦 is the closest workflow benchmark. Its public app description combines opening-auction tools, draggable interval/time-axis statistics, sector strength using weighted performance plus limit-up counts, a market-emotion model, live market narration, and a post-market review that organizes sector core stocks, profit/loss effects, and market-height ladders. The transferable idea is not one metric; it is continuity from pre-open -> intraday -> review.
- 同花顺 emphasizes real-time anomaly push, price/event alerts, market rises/falls, broad-market anomalies, hot sectors, and capital flow in one “market overview” workflow. The transferable pattern is alert plus context, not a detached event feed.
- 东方财富 combines Level-2/transaction restoration, theme/stock selection, chart overlays, order-book anomalies, and capital flow. Its public sector endpoint exposes sector return, main-net flow, and the leading stock/leading-stock return together. The transferable pattern is a sector row that already answers “板块多强、资金怎样、谁领涨.”
- 通达信 emphasizes customizable screeners/formulas, real-time anomaly alerts, leader-focused limit-up live coverage, and post-close money/dragon-tiger interpretation. The transferable pattern is power-user configurability and a short path from signal to stock investigation.
- 财联社 adds the missing catalyst layer: time-ordered news, hot-theme interpretation, anomaly reasons, and event calendars. The transferable pattern is to keep machine-detected behavior and explanatory catalyst evidence adjacent but visibly distinct.
- TradingView provides cross-market screeners, cloud alerts, and bar replay/replay trading. The transferable pattern for Cynexus is deterministic historical replay: place the user at 10:17, reveal only information known by then, and let them inspect what happened next.
- Across competitors, the strongest common structure is: environment/context -> ranked sector -> named leader/core stocks -> reason/evidence -> drill-down -> replay/review. Cynexus currently starts at pool breadth and therefore skips the user's first and last questions.

## Reusable Cynexus Capabilities

- Market Pulse already exposes current full-A turnover, rising/falling/flat counts, limit-up/down counts, main fund-flow fields, broad-index returns, 1-minute batch index K-lines, style-sector snapshots, sector leading stock/current leader return, sector fund flow, sector constituents, and a market heatmap.
- Market Sentiment already exposes a 0-100 composite and nine components: volatility, turnover, price strength, risk appetite, breadth, limit emotion, profit effect, sector breadth, and style risk. Current contracts appear primarily date-level, so an event-time regime snapshot still needs intraday persistence/resolution.
- The redesign should compose these existing domains rather than making Market Indicator re-collect or redefine full-market state. Market Indicator should own anomaly episodes and propagation evidence; Market Pulse/Sentiment should supply context.
- Existing sector snapshots already contain `leadingStock`, `leadingChangePct`, sector return, main net flow, and stock count. They can seed the first UI iteration, but they are insufficient for “who led first” without intraday time-series and event alignment.

## External Research Sources

- https://apps.apple.com/cn/app/id1071188962 (开盘啦 public app description, resolved through Apple Search API)
- https://apps.apple.com/cn/app/id303191318 (同花顺 public app description, resolved through Apple Search API)
- https://apps.apple.com/cn/app/id423525686 (东方财富 public app description, resolved through Apple Search API)
- https://apps.apple.com/cn/app/id907323628 (通达信 public app description, resolved through Apple Search API)
- https://apps.apple.com/cn/app/id1205990992 (TradingView public app description, resolved through Apple Search API)
- https://quote.eastmoney.com/center/boardlist.html#industry_board and its public board-ranking data endpoint
- https://q.10jqka.com.cn/thshy/
- https://www.tradingview.com/support/solutions/43000691889-how-do-i-turn-bar-replay-on/

## Final Planning Artifact

- `implementation-plan.md` contains the complete target architecture, five-second benchmark matrix/SLO, canonical contracts, formulas, offline validation, phased implementation, storage/API/file breakdown, deterministic buy/sell reference policy, Agent/AI boundaries, tests, Release rollout, rollback, and acceptance criteria.

## Research Errors

- The first competitor extraction command failed because PowerShell parsed `$k:` as an invalid variable reference; corrected it to `${k}:` before retrying.
- The guessed 同花顺 `/limit_up/` path returned 404; the analysis does not rely on that page and uses the official industry page and public app description instead.

## Runtime Acceptance Evidence (2026-08-05)

- Controlled real `market_indicator_benchmark` completed with eltdx, one round, pool size 8, batch size 160: 500 rows 3.1932s / 99.60% coverage; 1,500 rows 4.4991s / 99.73%; 3,000 rows 6.2669s / 99.73%; full 5,530 rows 9.0187s / 99.67%.
- The benchmark is provider + normalization + transmission calculation only. The persisted `market_indicator_refresh` workflow is the production SLO evidence: recent full-market runs took 20.67–27.05s normally, with a 132s outlier; all were marked `degrade_cadence` and reported effective cadence 26–32s.
- Production scheduler cadence is therefore set to 30 seconds until a warm long-running provider session replaces the compatibility refresh path. The 5-second target remains a benchmark/shadow target, not a claim about current live throughput.
- Real `market_indicator_outcome_finalize` read 454 persisted episodes and captures and wrote 454 outcome rows; because evaluation ran before 5/15/30-minute forward windows were complete, all were correctly classified `insufficient_data` with `未来窗口数据不足`.
- `watchlist preview -> confirm` initially exposed a missing database enum/check constraint. Migration `20260805_allow_market_indicator_watchlist_kind.sql` added `market_indicator_review`; after redeployment, confirm wrote list id `1785912505695` and item id `1785912505696` successfully.
- After the cadence migration, one pre-existing refresh run had remained `running` since 14:47 with its one-hour lease. Data Executor restart marked it interrupted; the explicitly identified lock was expired, and the next two scheduled full-market refreshes completed successfully in 18.85s and 22.86s with `benchmark_gate=pass` and effective cadence 30s.
- The Data Executor image was rebuilt/recreated once more after the jobs catalog change; `/internal/health` is healthy and no active refresh lock remains outside trading hours.

## Dual-Speed Cadence Decision (2026-08-05)

- A direct full-market cadence reduction is unsafe: complete compatibility refreshes measured 18.85–27.05 seconds and include PostgreSQL/Redis work, so a 15-second schedule can overlap or degrade under provider variance.
- The user-visible need is faster signal awareness, not that every 10-second tick must be a complete industry confirmation. Add a priority radar over the historical limit-up/down cohort (bounded to 1,500 symbols by default), publish only to a dedicated Redis scope, and do not flush PostgreSQL on every cycle.
- Priority results must carry `universeMode=historical_limit`, `scope=priority`, and `fullConfirmationRequired=true`; the transmission engine must use full-market eligible counts so partial rows remain `emerging/wait_confirmation` and cannot become confirmed leader/sector conclusions.
- The full-market job remains the authoritative confirmation/replay line at 30 seconds. UI continues polling at 5 seconds but displays scope, freshness, coverage, and confirmation state explicitly.
