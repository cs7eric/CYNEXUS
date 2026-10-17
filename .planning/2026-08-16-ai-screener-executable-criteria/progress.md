# Progress

## 2026-08-16

- Began follow-up implementation for as-of-date consistency, default investable universe, flexible AI volume conditions, and deterministic candlestick patterns. No source files have been changed yet.
- Replaced the fixed Screener quote history argument with a condition-driven session requirement (31 by default, 61/253 only for active 60/252-day filters or sorts). Technical daily-fact reads now require the exact actual quote date; fund flow and industry enrichment were changed to exact-date queries rather than per-row/source fallback.
- Added default ChiNext and STAR exclusions to the typed screener contract, strategy replay, AI adapter, and Agent tool contract. Added volume-range fields and an eight-pattern daily candlestick catalogue across the Market contract, AI criteria policy, strategy contract, and Screener UI. Compilation and focused-test repair remain pending.
- Completed the time-consistency implementation: daily quote history is demand-driven (31/61/253 sessions), and industry, fund-flow, and daily-pattern enrichments only join on the actual ClickHouse trade date. Current concept/style memberships are explicitly surfaced as current classifications.
- Completed the default investable universe: ChiNext and STAR are excluded unless explicitly included. Selecting either board in the UI automatically clears that board's exclusion. Strategy replay, AI candidate execution, and the Agent tool use the same defaults.
- Replaced exact AI volume multiples with editable bounded ranges. The supported daily candlestick catalogue is bullish/bearish engulfing, hammer, shooting star, doji, up/down gap, and inside bar; multi-swing patterns remain research-only until historical swing detection is governed.
- Added intent-planning prompt v5 and its Release migration entry. Verified Market domain tests (15), AI intent tests (8), and Agent contracts (11), version synchronization, UI lint/build, direct screener behavior, and Release deployment of market and ai as `1.1.400-260816-RELEASE`. Both services report `UP`; the intent binding resolves to active prompt v5.

- Started audit of AI intent planning, the Screener UI, and trusted daily pattern data.
- Confirmed the UI/API mismatch: intent-plan can propose only the existing scalar catalogue, and its apply callback maps only three shape fields.
- Confirmed that the durable daily fact contains the OHLCV fields needed for a bounded technical-pattern expansion.
- Identified the concrete AI UX issue: applying an interpreted plan neither presents all executable values nor switches to a clear editing surface.
- Traced the complete Market API boundary that must evolve with the typed technical pattern contract.
- Extended the Market domain technical-pattern contract and daily-OHLCV derivation for MA alignment, high breakout, ATR20, consecutive bullish candles, and volume-versus-MA5. Remaining work is API/UI propagation and tests.
- Confirmed strategy replay, the AI candidate adapter, and the bounded Agent tool are downstream contract consumers of the same criteria.
- Added the expanded technical criteria to the Market HTTP contract, UI request model, condition builder, AI allow-list, strategy serialization, and AI candidate API call. The AI panel now requires conversion first, renders every proposed executable value, and applies it into the editable Indicators tab.
- Updated the bounded autonomous screener schema and capability version to expose the same explicit technical criteria; it remains a daily-close, read-only query with hard limits.
- Verified current durable daily-fact coverage and found only 30 sessions. Investigating the materialization job's safe historical backfill boundary before releasing the 120-session breakout control.
- Resumed with the implementation present as untracked source files in a dirty worktree. The bounded high-breakout control remains capped at 20 sessions to match the verified daily-fact coverage; release and runtime endpoint validation are the remaining work.
- Release validation confirmed the new market technical criteria are executable against the 2026-08-14 daily-close snapshot. A real v1 AI-plan call exposed incorrect handling of the representative volume-and-candle phrase; added prompt v2 plus domain-level semantic reconciliation and a regression test before the final AI-only redeploy.
- Verified `custom / gpt-5.4` against the configured provider account and made it the one-time default for both Screener AI bindings. Added an ordered, independent fallback route (`custom`, `xhhai`) with provider-specific model overrides, so a user-selected provider is never retried as its own fallback.
- Added prompt v3 and policy reconciliation for standalone volume intent: phrases such as `三倍爆量` now produce executable `minVolumeVsMa5Multiple=3`, while `持续缩量然后三倍爆量` continues to produce the contraction-relative breakout-volume condition.
- Release-deployed the AI service at `1.1.400-260816-RELEASE`; health is `UP`. Runtime calls validated the representative plan (`3` contraction sessions, `3` breakout multiple, `4%` body, `5%` daily change), an AI screen on the trusted daily-close snapshot, and standalone volume planning (`minVolumeVsMa5Multiple=3`, prompt v3).

## Errors

| Error | Attempt | Resolution |
| --- | --- | --- |
| A combined patch contained two updates for the same UI file | 1 | Split the technical-controls and apply-callback changes into separate patches. |
| Market domain compilation failed because strategy replay had no integer parameter helper | 1 | Added the bounded integer conversion helper and a technical-pattern hard-filter test. |
