# Findings

- CodeGraph index is present and up to date.
- Existing Market routes are under `/market/*`; sidebar children are configured in `src/config/sidebar.ts`.
- Existing services expose `fetchMarketOverviewEltdx`, `fetchMarketOverviewAkshare`, `fetchMarketPulseIndexReturns`, `fetchStyleSectors`, and `fetchMarketIndicators`.
- A-share direction convention is red for up and green for down; the new page must pair color with signs/icons/text.
- Existing dirty changes are extensive and unrelated; edits must remain scoped to the new page plus route/sidebar.
- The Market section has six distinct data products: Pulse, Sentiment, Margin Trading, ETF, Futures Position, and Market Indicator.
- Overview should use one compact row per product with a primary reading, freshness, and deep-link; detailed charts/tables belong on child routes.
- Available summary endpoints: `fetchMarketSentimentIndex`, `fetchMarginLatest`, `fetchMarketEtf`, `fetchFuturesPositionLatest`, plus existing pulse and indicator endpoints.
