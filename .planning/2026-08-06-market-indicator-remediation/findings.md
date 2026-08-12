# Findings: Market Indicator Remediation Proposal

- The current page mixes a full-market overview with priority-first transmission records but does not expose capture identity, scope, universe, coverage, or freshness together.
- `all` is labelled as a limit-up/down pool while it is populated from an unfiltered, capped overview list.
- Rapid events use adjacent-capture daily-percent deltas; transmission uses adjacent price return. These are not the same contract and neither is presented with a window, sector-relative, market-relative, or activity baseline.
- The related-sector score is mapping confidence plus current direction alignment, with no temporal evidence and `overlap_count` hard-coded to zero. It cannot answer why a sector was derived.
- The prior design correctly specifies `MarketContext -> SignalEvent -> SectorEpisode -> Outcome`, two operating modes, provenance and evidence expansion. The current page has only parts of that design.
- Live API inspection on 2026-08-06 returned 5,512 full-market snapshots, 90 industries, zero current events, and no transmission episodes/advice/outcomes. The page cannot currently support the promised radar/replay workflow from that response.

## Phase 7 Research: Reusable Data Capabilities and Gaps

- The UI already exposes `/api/market/pulse/index-kline/batch?interval=1m` for `000001`, `399001`, and `399006`. The Java path reads ClickHouse `ch_dwd_kline_1m` and returns timestamped OHLCV/turnover points. This is sufficient for the market benchmark lane in a replay, but it is currently isolated in Market Pulse.
- Stock chart queries already read `ch_dwd_kline_1m` for a single stock and trade date, with aggregation to 1m/5m/15m/30m. The existing SQL is a reusable source adapter, not a sector-wide replay contract; a batch bounded query or precomputed context table is required to avoid N+1 requests for an episode.
- Market Sentiment has daily/history endpoints for MA breadth, turnover activity, limit emotion, risk appetite, sector breadth, profit effect, style risk, and the composite index. These are valuable day-level context and validation signals, but they are not minute-aligned and must not be displayed as if they describe an event minute until a minute context job exists.
- Market Indicator persistence already contains minute captures, event timestamps/event minutes, industry snapshots, `first_limit_up_at`, `first_limit_up_code`, streak-related daily limit-stock data, and limit status. It does not yet persist a lifecycle episode, effective-seal confirmation, response waves, forward outcome horizons, or a synchronized market context row.
- `market_limit_daily_stocks` currently carries `trade_date`, code/name, category, streak, change percent, and limit prices. It is a daily ladder source, not an intraday first-seal source. Intraday first-board timing must come from indicator events/snapshots or a dedicated limit-anchor table.
- Existing Agent exposure (`market.indicator`) is bounded read-only and already describes transmission/replay intent, but its operations only cover latest, industry detail, and transmission. New replay endpoints should be added to the same bounded tool only after their contracts and freshness semantics are stable; an AI explanation is a separate explicit capability, not an automatic Agent-side narrative.

## Product conclusion

The next change should not add more right-panel metrics to the existing snapshot. It should introduce an event cursor and deterministic lifecycle/outcome model. The first release can use index 1m plus the existing captured candidate universe; sector-wide 1m coverage should be reported explicitly and expanded incrementally. AI should explain a computed evidence packet only after the quantitative replay is useful without AI.
