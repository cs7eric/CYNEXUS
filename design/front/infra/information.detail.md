# Market Information

## Overview

Market Information presents the market brief, priority events, deterministic industry impact, risk/watchlist signals, company events, and the raw 7x24 feed.

## Route

`/information`

## Module

`cynexus-ui/src/views/information/index.tsx`

## Data Sources

- Market Information feed and event clusters from the Market service
- AI-generated brief and event analysis from the AI pipeline
- Deterministic industry mappings and representative related stocks from the Market response

## APIs

- `fetchMarketInformation`: paginated feed, brief, and impact options
- `fetchMarketInformationEvent`: event Sheet details
- `fetchMarketInformationAnalysisStatuses`: asynchronous analysis polling
- `requestMarketInformationAnalysis`: queue or re-run event analysis

## Components

- `MarketBrief`: compact market read and key events
- `MarketDecisionWorkspace`: priority events, impact map, risk/watchlist, and trigger POC
- `MarketInformationTable`: filtered, paginated priority feed
- `MarketEventSheet`: event detail and analysis actions

## Data Flow

Market API -> `cynexus-ui/src/services/information.ts` -> page orchestration -> Information components.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/information.detail.md`
- `design/front/reuse/reuse.md`

The compatibility route `/market/information` must remain a redirect unless all external links are migrated.
