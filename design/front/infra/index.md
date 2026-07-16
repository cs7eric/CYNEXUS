## Market Information

- Route: `/information`
- Module: `cynexus-ui/src/views/information/index.tsx`
- Detail: `./information.detail.md`
- Data Source: Market Information feed, event clusters, AI brief, deterministic industry mappings
- APIs: `/api/market/information`, `/api/market/information/events/{clusterId}`, `/api/market/information/events/status`
- Components: `MarketBrief`, `MarketDecisionWorkspace`, `MarketInformationTable`, `MarketEventSheet`, `RawWireList`

## Maintenance

- The legacy `/market/information` route is a compatibility redirect.
- Keep the page and service under the `information` module; do not add new implementation files under `views/market` or `services/market`.
