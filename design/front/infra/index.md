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

## AI Analyzed Information

- Route: `/information/analyzed`
- Module: `cynexus-ui/src/views/information/index.tsx` (`MarketInformationAnalyzedPage`)
- Detail: `./information-analyzed.detail.md`
- Data Source: Market Information feed filtered to completed AI analysis
- APIs: `fetchMarketInformation`, `fetchMarketInformationEvent`
- Components: `MarketInformationTable`, `MarketEventSheet`

## AI Provider

- Route: `/ai/provider`
- Module: `cynexus-ui/src/views/ai/provider/index.tsx`
- Detail: `./ai-provider.detail.md`
- Data Source: AI provider and capability configuration APIs
- APIs: `fetchAiProviders`, `fetchAiBindings`, provider CRUD, binding upsert
- Components: Provider editor, capability binding panels

## AI Usage

- Route: `/ai/usage`
- Module: `cynexus-ui/src/views/ai/usage/index.tsx`
- Detail: `./ai-usage.detail.md`
- Data Source: Langfuse observations through the AI usage API
- APIs: `fetchAiUsage`
- Components: `SummaryMetric`, `CapabilityRow`, `ProviderHealthRow`, recent calls table

## AI Prompt

- Route: `/ai/prompt`
- Module: `cynexus-ui/src/views/ai/prompt/index.tsx`
- Detail: `./ai-prompt.detail.md`
- Data Source: AI prompt registry APIs
- APIs: `fetchAiPrompts`, prompt CRUD
- Components: Prompt list, prompt editor dialog
