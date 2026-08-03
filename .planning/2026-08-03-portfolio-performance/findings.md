# Findings

- Current overview has marketValue, availableCash, totalAssets, totalPnl, realizedPnl, unrealizedPnl, costBasis, and positions.
- Current return component only renders a horizontal bar chart of current position unrealized P&L; no time-series API exists yet.
- Available cash is stored in a dedicated portfolio account table and is manually edited.
- Existing `StockChartQueryDomainService.klines` exposes up to 520 daily bars for stocks and ETFs, which is enough for daily/monthly and a bounded custom range without adding another data source.
- Portfolio trades are queryable as active records and the existing ledger domain service can calculate realized/unrealized P&L for any prefix of the trade timeline.
- Historical performance can therefore be calculated on demand from trade prefixes plus carry-forward closing prices; no scheduler, MQ, or new snapshot table is required for this read feature.
