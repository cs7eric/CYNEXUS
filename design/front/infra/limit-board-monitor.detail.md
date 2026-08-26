# Data Collector · Limit Board Monitor

## Overview

展示涨停和跌停股票按交易时段规整后的封单额、封板持续时间、开板/回封次数、覆盖率和封单趋势。页面不展示盘中 Redis 原始快照。

## Route

`/data-collector/limit-board-monitor`

## Module

`cynexus-ui/src/views/data-collector/limit-board-monitor/index.tsx`

## Data Sources

- `market_indicator_limit_board_session_summaries` PostgreSQL read model
- Morning and afternoon 5-second capture/finalize jobs in `cynexus-data`

## APIs

- `fetchLimitBoardSessionSummaries`: bounded query by trade date, session, direction, and row limit
- `GET /api/market/indicator/limit-board/summaries`: Java gateway for the persisted summary read model

## Components

- `StockBoardCard`: compact card per stock with identity, seal metrics, and an overlaid timeshare
- `OverlayTimeshareChart`: ECharts overlay of TDX timeshare price and sealed-amount series on the same session clock axis

## Lib

- `format.ts`: display formatters, session clock/minute axis helpers, sealed-amount series
- `timeshare.ts`: page-local TDX timeshare fetch/parse for card overlay

## Data Flow

Capture jobs -> Redis session cache -> session finalize aggregation -> PostgreSQL summary table -> Java query API -> UI summary table.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/limit-board-monitor.detail.md`
- `design/front/reuse/reuse.md`

The page intentionally does not query raw Redis capture keys or expose source snapshots.
