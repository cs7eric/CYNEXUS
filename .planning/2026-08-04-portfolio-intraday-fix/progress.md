# Progress

## 2026-08-04

- Resumed from prior implementation summary.
- Read backend and Release skills plus deployment guidance.
- Created this task plan and recorded the known calculation and data issues.
- Changed `PortfolioOverviewAppService` to always invoke the minute-aware domain overload, including when the minute series is empty so execution-price fallback is used.
- Changed trade-time quote lookup to reject future quotes and use the execution price until an at-or-before quote exists.
- Added focused tests for empty-minute fallback and for a trade occurring before the first intraday quote.
- Initial focused Maven invocation without reactor dependencies failed because `cynexus-market-common` was not installed; retrying with `-am`.
- The first reactor retry exposed PowerShell argument parsing of the `-D` property; subsequent Maven calls will use quoted arguments through `mvn.cmd`.
- Reactor compilation succeeded; two new assertions failed because the expected values omitted the opening 100-share movement before the sale. Correct expected P&L is 380 in both cases.
- Focused domain test passed: 16/16.
- Focused application-service test passed: 1/1.
- Verified the six 2026-08-04 trades in PostgreSQL and corrected the four imported midnight timestamps to 09:30:00, 09:33:40, 09:45:14, and 09:51:48 UTC; the two existing afternoon timestamps were retained.
- Version synchronization passed at `1.1.398-SNAPSHOT`.
- Full Maven compile passed across all 54 reactor modules.
- The first full Release rebuild applied idempotent migrations but its Maven packaging phase failed in the unrelated pre-existing `cynexus-ai-infra` test compile (`StringRedisTemplate` vs `RedisCacheOperations`). The Release script's `-DskipTests` still compiles tests, so an alternate artifact build with `-Dmaven.test.skip=true` is required before rerunning the Release stack without `-Build`.
- Full backend package succeeded with `-Dmaven.test.skip=true`; all Release artifacts are now available for Docker image rebuild.
- Full Release application stack rebuilt and recreated at `1.1.398-260804-RELEASE` for core, market, ai, gateway, knowledgebase, wechat, and ui.
- Release verification passed: all seven application containers are up, the six corrected trade timestamps are returned by the market API, overview and performance both return `todayPnl=93.3`, and the UI returns HTTP 200.
- Minute data limitation remains: the 2026-08-04 ETF minute table has no rows for these symbols, so the deployed calculation uses execution-price fallback; ClickHouse currently contains 2026-08-04 minute rows only for index symbols.
- `git diff --check` reported no whitespace errors in the changed calculation files.
- New request: change source selection so live Tencent quotes drive intraday calculations during market hours, while post-close calculations consult today's ClickHouse 1m data when available; include fully closed-today symbols in the live quote universe.
- Implemented source selection with an injectable Shanghai clock: no 1m query before 15:00, 1m query at/after 15:00, and latest 1m point overrides the post-close mark when present.
- Expanded realtime quote lookup to every active-trade symbol, including symbols fully sold today, using a positive lookup shell so the Tencent gateway does not filter them out.
- Focused tests passed: PortfolioOverviewAppServiceTest 3/3 and PortfolioPerformanceDomainServiceTest 16/16.
- First full redeploy exposed a Spring constructor-selection error in the market container; the production constructor is now explicitly `@Autowired` and the full Release stack will be rebuilt/recreated again before final verification.
- Rebuilt all backend artifacts with `-Dmaven.test.skip=true`, rebuilt the UI, and redeployed all seven Release services at `1.1.398-260804-RELEASE`.
- Final verification passed: all seven Release containers are up, UI HTTP 200, market overview HTTP 200, market performance HTTP 200, and the market log reports a completed application startup with no constructor failure.
- Final API data after the date rollover reports `todayPnl=0` for 2026-08-05 (no trades that day); the 2026-08-04 performance point is present with `periodPnl=125.1`.
- New request: portfolio imports must process only transactions whose Shanghai-local trading date equals today.
- Implemented review-time exclusion and confirmation-time protection for non-today candidates. The exclusion warning is shown to the user, and old/future rows are never saved to `portfolio_trade`.
- Added focused coverage for a non-today candidate: review marks it `EXCLUDED`, confirmation skips it, and the task can still complete without persisting the invalid trade.
- Rebuilt all backend artifacts with `-Dmaven.test.skip=true` and rebuilt the UI successfully.
- Rebuilt and recreated the complete Release stack at `1.1.398-260805-RELEASE` for core, market, ai, gateway, knowledgebase, wechat, and ui.
- Release verification passed after startup warm-up: all seven application containers are up, UI HTTP 200, portfolio overview HTTP 200, and portfolio performance HTTP 200.
- The first immediate API probe returned 502 while gateway and market were still starting; a retry after 20 seconds returned 200 for both endpoints.
