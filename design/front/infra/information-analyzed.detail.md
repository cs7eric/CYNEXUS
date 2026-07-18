# AI Analyzed Information

## Overview

Shows Market Information records whose latest configured event analysis completed successfully, with the same event detail and structured analysis view used by the main feed.

## Route

`/information/analyzed`

## Module

`cynexus-ui/src/views/information/index.tsx` (`MarketInformationAnalyzedPage`)

## Data Sources

- Market Information feed filtered by completed AI analysis
- Event analysis detail and source members

## APIs

- `fetchMarketInformation`: requests `analysisStatus=SUCCEEDED` with server-side pagination
- `fetchMarketInformationEvent`: loads the selected event's structured analysis

## Components

- `MarketInformationTable`: paginated analyzed information list
- `MarketEventSheet`: structured event analysis and source detail

## Data Flow

Market API -> server-side `SUCCEEDED` filter -> `MarketInformationTable` -> `MarketEventSheet`.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/information-analyzed.detail.md`
- `design/front/reuse/reuse.md`
