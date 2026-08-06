# Portfolio intraday performance fix

## Goal

Calculate same-day portfolio performance in transaction-time order so shares sold no longer participate in later price movement, including the 2026-08-04 trades, then rebuild and redeploy all affected Release services.

## Phases

- [complete] Inspect current changes, callers, transaction data, and runtime state
- [complete] Fix minute-level calculation wiring and transaction timestamps
- [complete] Run focused tests, compile, and version checks
- [complete] Rebuild and redeploy the full Release application stack
- [complete] Verify API/UI behavior and workspace cleanliness
- [complete] Separate live-session realtime valuation from post-close 1m valuation, including closed-today symbols
- [complete] Test, rebuild, redeploy, and verify the full Release stack
- [complete] Restrict portfolio trade imports to Shanghai-local transactions from the current day
- [complete] Compile, redeploy the full Release stack, and verify import behavior

## Decisions

- Keep business calculation in the portfolio domain service.
- Use minute prices when available and transaction prices as the same-time fallback.
- Treat the 2026-08-04 imported transaction timestamps as data corrections because the calculation depends on ordering.
- During the Shanghai A-share session, use the live quote gateway; after the 15:00 close, use today's 1m series when available and retain live/daily fallback when it is not.
- Import review marks non-today candidates as excluded, and confirmation never persists them; the current day is evaluated in Asia/Shanghai.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| FastCtx/CodeGraph tools unavailable in this session | 1 | Use the documented PowerShell fallback and inspect the closest guidance files manually |
| Focused Maven test could not resolve `cynexus-market-common` | 1 | Re-run with `-am` so Maven builds required reactor dependencies |
| PowerShell parsed a Maven `-D` property as a lifecycle phase | 1 | Use `mvn.cmd` and quote Maven property arguments |
| New fallback tests expected only post-sale movement | 1 | Correct expectations to include the opening holding's movement up to each execution price |
| Full Release build stopped at pre-existing `RedisAgentRateLimitConcurrencyTest` type mismatch | 1 | Build backend artifacts with `-Dmaven.test.skip=true`, then run the Release script without `-Build` so Docker rebuild/recreate still covers every service |
| First redeploy market container restarted because the added test-clock constructor made Spring see multiple constructors | 1 | Annotate the production constructor with `@Autowired`, rebuild all Release artifacts/images, and redeploy the full stack |
| Initial import-date test compile rejected `ZoneId` passed to `OffsetDateTime.withOffsetSameInstant` | 1 | Use the Shanghai fixed `ZoneOffset` for instant-to-trading-date conversion |
