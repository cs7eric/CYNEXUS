# Progress

## 2026-08-04

- Created the implementation plan and began tracing the existing financial image analysis and transaction recognition flows.
- Confirmed the financial flow is a persisted `FinanceAiCapture` API, while the trade flow is currently an asynchronous `PortfolioImport` API. Both ultimately use the AI vision endpoint, with different stable capabilities.
- Added `POST /api/market/portfolio/trade-analysis`, accepting multipart `image` or `file`, validating the image, invoking the existing `portfolio_trade_screenshot_parse` vision gateway, validating the returned JSON, and returning the same `status/content` style as the generic vision API.
- Market application compile passed. Two focused domain tests passed after correcting the test dependency style used by this module.
- Version synchronization passed at `1.1.398-SNAPSHOT`.
- Rebuilt and restarted `cynexus-release-market` as `1.1.398-260804-RELEASE`; the container is running and the new endpoint is reachable. A no-file probe returned the expected API validation envelope.
- Diagnosis: the failed task had `attempt_count = 1` and stopped at `PORTFOLIO_PARSE_INVALID`; schema errors were caught as non-retryable `IllegalArgumentException`. Retry queues are configured for 30 seconds, 2 minutes, and 10 minutes, but were bypassed for this error class.
- Changed malformed/semantically invalid AI trade JSON to raise a retryable `PORTFOLIO_AI_RESPONSE_INVALID` vision failure. Focused tests: 9 passed; market compile passed; Release market rebuilt and is running with the change.
