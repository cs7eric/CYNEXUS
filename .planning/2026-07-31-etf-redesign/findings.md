# ETF Redesign Findings

## Current implementation
- `market-etf/index.tsx` renders header, 4 summary metrics, trend chart + breadth, rotation table, mapping panel, then two very wide tables.
- The API already exposes price/change, amount, IOPV premium, turnover, shares/share delta, subscription proxy, buy/sell volume, order-size net inflows, rotation aggregates, breadth, and mapping coverage.
- Current tables render 14 columns at a minimum width of 1500px and show the first 30 rows without sort/filter/search controls.
- Existing page labels explain data caveats, but the hierarchy makes the user scan across several large blocks before reaching an action.

## Competitor patterns to absorb
- Eastmoney: watchlist-first market lists, fast sorting/search, and entry points into quote/valuation/flow context.
- Snowball: symbol search and a persistent path from market list to selected-instrument context.
- TradingView: screener, multi-condition filters, configurable columns, comparison, and chart linkage.
- justETF: fees, fund size, tracking quality, holdings, exposure, risk, distribution, and comparison.
- Institutional terminals: separate market pulse from instrument research, with pinned identity columns and saved research presets.

## Capability gaps worth adding next
- Search and saved ETF watchlist/presets.
- Sortable/filterable table with compact presets: high volume, net inflow, premium anomaly, breadth leaders/laggards.
- Selected ETF detail drawer with price/IOPV/premium, liquidity, share change, and flow breakdown.
- Time-range and series toggles for the trend chart.
- Explicit data freshness and source badges per block; stale/error/empty states.
- Compare mode and alerting are valuable later, but require backend/user-state work and should not block this UI redesign.

## Design direction
Quiet research desk: a compact header with freshness and controls, a single regime strip, a two-column “what changed” workspace, then tabbed rankings and wide tables. Keep neutral surfaces, one information accent, and red-up/green-down semantics.
