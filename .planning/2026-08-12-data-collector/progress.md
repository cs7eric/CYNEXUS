# Progress

## 2026-08-12

- Inspected UI route, sidebar, command palette, existing Market Indicator page, Java market indicator query flow, and the new Python/PostgreSQL limit-board summary contract.
- Confirmed the migration target is `src/views/data-collector/market-indicator/` and the new monitor target is `src/views/data-collector/limit-board-monitor/`.
- Moved the Market Indicator page and private components under `data-collector/market-indicator`.
- Added `/data-collector/market-indicator`, `/data-collector/limit-board-monitor`, and compatibility redirects for `/market/indicator` and `/data-collector`.
- Added the bounded Java query endpoint `/api/market/indicator/limit-board/summaries` and the UI service/page for persisted session summaries.
- `pnpm build` passed; focused TypeScript ESLint reports inherited pre-existing effect-rule errors in the migrated page and global files. Full lint reports 110 existing errors across the repository.
- `mvn -pl cynexus-market/cynexus-market-application/cynexus-market-application-controller -am -DskipTests compile` passed.
