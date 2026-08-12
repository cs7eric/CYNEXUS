# Data Collector · Market Indicator

## Overview

盘中雷达与盘后复盘工作台，展示市场快照、个股异动、涨跌停池、行业结构、传导证据和交易日回放。

## Route

`/data-collector/market-indicator`

## Module

`cynexus-ui/src/views/data-collector/market-indicator/index.tsx`

## Data Sources

- Market Indicator Java API
- Market Indicator PostgreSQL/Redis read models through the Java service

## APIs

- `fetchMarketIndicators`: current or selected trade-date snapshot
- `fetchMarketIndicatorTransmission`: transmission episodes and advice
- `fetchMarketIndicatorReplayTimeline`: replay evidence timeline
- `fetchMarketIndicatorIndustryDetail`: industry drill-down

## Components

- `MarketContextStrip`: current market context
- `TransmissionPanel`: transmission evidence and watchlist action
- `IndustryIndicatorTable`: industry ranking
- `IndustryDetailPanel`: constituent and relation drill-down
- `LimitBoardList`: limit-up and limit-down lists
- `IndicatorTable`: filtered stock indicator table
- `EventFeed`: recent event feed
- `ReplayTimeline`: replay timeline
- `ReplayReviewPanel`: replay review summary

## Lib

- Page-local filtering and display formatters remain in the page entry because they are only orchestration logic for this workbench.

## Data Flow

Market Indicator API -> page state/filtering -> domain panels and tables -> UI rendering.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/market-indicator.detail.md`
- `design/front/reuse/reuse.md`

The legacy `/market/indicator` route redirects here for compatibility.
