# Progress Log

## 2026-07-31
- Read project instructions, product/design context, and frontend design guidance.
- Audited the existing ETF route, components, and service contract.
- Recorded competitor-derived patterns and prioritized gaps in `findings.md`.
- First UI build caught a `verbatimModuleSyntax` type-only import error; corrected the import shape.
- Targeted ESLint and `pnpm run build` pass.
- Synchronized all 41 Maven POM SNAPSHOT versions and UI package version to `1.1.379-SNAPSHOT`.
- Release UI rebuilt as `cynexus-ui:1.1.379-260731-RELEASE` and recreated successfully.
- Release endpoint `/market/etf` returned HTTP 200; Playwright was unavailable in the workspace.
- Rebuilt the Release UI after the final stale-selection fix; container remains healthy and serves the route.
- Follow-up polish moved the research toolbar directly beneath the page header as a sticky first-viewport control; Release image rebuilt again.
- Implemented the competitor-informed opportunity panel, compact table columns, theme activity bars, and 5D/10D trend switch.
- Release image rebuilt as `cynexus-ui:1.1.380-260731-RELEASE`; route and ETF API both returned HTTP 200.
- Replaced the rejected stacked dashboard with a table-first ETF research workbench and compact market tape.
- Added scope/search/preset filtering, deterministic sortable columns, persisted research view settings, a fixed identity column, and full result rendering.
- Added a persistent insight rail for opportunities, selected ETF detail, theme rotation, and 5D/10D trend context.
- Added explicit loading, retry, empty, data-source, freshness, and proxy-methodology states.
- Re-ran targeted ETF ESLint and the full UI build successfully.
- Verified all 41 Maven POM versions and UI package version at `1.1.385-SNAPSHOT`.
- Rebuilt and recreated the Release UI as `cynexus-ui:1.1.385-260731-RELEASE`.
- Captured a 1440x1100 headless Chrome screenshot showing the live ETF data rendered in the redesigned workspace.
- Captured a 390x844 mobile screenshot confirming the controls stack cleanly and wide market data remains horizontally scrollable.
- Verified `/market/etf` and `/api/market/etf?historyDays=10&rowLimit=180` return HTTP 200 through the Release UI container.
- Follow-up polish compressed the scope tabs/search/preset toolbar to a 28px control row.
- Added a compact market-reaction readout explaining breadth, broad/sector flow resonance, IOPV pressure, liquidity, and leading-theme concentration.
- Adapted the readout to a two-column mobile grid and revalidated the `1.1.386-260731-RELEASE` UI image.
- Reduced ETF scope-tab horizontal padding from `px-2.5` to `px-1.5` while preserving the shadcn-style focus and selected states.
- Rebuilt the synchronized `1.1.387-260801-RELEASE` UI image and verified the route and ETF API through the running container.
