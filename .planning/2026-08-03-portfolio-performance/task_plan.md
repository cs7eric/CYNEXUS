# Portfolio performance history

## Goal

Add daily, monthly, and custom-range portfolio return statistics with a history chart while keeping cash and holdings separated.

## Phases

- [complete] Inspect existing portfolio APIs, market price history, and migration conventions
- [complete] Design and implement on-demand performance/query backend
- [complete] Add UI range controls, summary metrics, and history chart
- [complete] Run focused tests, build, release deploy, and commit each feature

## Verification

- Backend compile: `mvn -pl cynexus-market/cynexus-market-application/cynexus-market-application-controller -am -DskipTests compile` passed.
- Domain tests: 10 focused tests passed.
- UI lint passed for changed portfolio files.
- UI build passed.
- Release `1.1.398-260803-RELEASE` rebuilt for market and UI.
- `/api/market/portfolio/performance` returned HTTP 200 for daily, monthly, and custom ranges.

## Decisions

- Prefer existing market price history and portfolio trade records; do not introduce a second scheduler or MQ for this read path.
- Keep manually edited available cash as an account input and show cash-flow-aware return metrics separately from asset value.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| planning-with-files repository path absent | 1 | Used installed skill under `C:\Users\Cs7er\.agents\skills` |
