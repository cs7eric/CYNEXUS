## Page: Market Information

### Components

#### MarketBrief
- Path: `cynexus-ui/src/views/information/components/market-brief.tsx`
- Purpose: Render the current market stance, key events, signals, risks, and uncertainties.
- Reuse Scope: Same domain pages
- Props Summary:
  - `brief`: Market Information brief payload
- Notes: Parses the persisted brief JSON and keeps summary presentation independent from page fetching.

#### MarketDecisionWorkspace
- Path: `cynexus-ui/src/views/information/components/market-decision-layers.tsx`
- Purpose: Render the priority event stream, impact mapping, risk/watchlist, and the upcoming-trigger placeholder.
- Reuse Scope: Current page only
- Props Summary:
  - `brief`: Brief payload used by risk/watchlist
  - `items`: Current priority feed rows
  - `onSelectEvent`: Event Sheet callback
- Notes: The upcoming-trigger slot intentionally does not invent dates or add an unapproved provider.

#### MarketInformationTable
- Path: `cynexus-ui/src/views/information/components/market-information-table.tsx`
- Purpose: Render the filtered and paginated priority information feed.
- Reuse Scope: Same domain pages
- Props Summary:
  - `items`: Visible information rows
  - `pagination`: Server pagination state
  - `onSelectEvent`: Event Sheet callback
- Notes: Keeps pagination and table layout independent from the page-level filters.

### Lib Methods

#### fetchMarketInformation
- Path: `cynexus-ui/src/services/information.ts`
- Purpose: Fetch the paginated Market Information feed and current brief.
- Input: Page, page size, and optional industry code
- Output: `MarketInformationFeed`
- Reuse Scope: Same domain pages

## Page: AI Configuration

### Components

#### SummaryMetric
- Path: `cynexus-ui/src/views/ai/usage/index.tsx`
- Purpose: Render a labeled AI usage metric with icon and supporting detail.
- Reuse Scope: Same domain pages
- Props Summary:
  - `label`: metric name
  - `value`: formatted metric value
  - `detail`: supporting context
- Notes: Kept local to the usage surface until another AI observability page needs it.

#### ProviderHealthRow
- Path: `cynexus-ui/src/views/ai/usage/index.tsx`
- Purpose: Render request success rate and failure count for a provider/model pair.
- Reuse Scope: Same domain pages
- Props Summary:
  - `provider`: display provider name
  - `model`: display model name
  - `requests`, `succeeded`, `failed`: health counters
- Notes: Uses the existing product status colors.

### Lib Methods

#### fetchAiUsage
- Path: `cynexus-ui/src/services/ai/usage.ts`
- Purpose: Fetch Langfuse-backed AI usage observations and aggregates.
- Input: Optional UTC `from`, `to`, and `limit` query parameters
- Output: `AiUsagePage`
- Reuse Scope: Same domain pages
