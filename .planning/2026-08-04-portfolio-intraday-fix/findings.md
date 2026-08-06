# Findings

## Current diagnosis

- The domain service has a new minute-level calculation path that orders trades, applies price movement before each trade, and updates shares immediately after the trade.
- `PortfolioOverviewAppService` still calls the old overload when minute data is empty, so the transaction-price fallback is bypassed.
- Four 2026-08-04 transactions currently have midnight timestamps in persisted data; two later trades retain concrete times. This prevents correct chronological matching.

## 2026-08-04 transactions requiring timestamp correction

- 159796 sell 900 at 09:30:00, database id `1785806176451`
- 513330 sell 2000 at 09:33:40, database id `1785806176450`
- 159029 sell 900 at 09:45:14, database id `1785806176449`
- 515850 sell 500 at 09:51:48, database id `1785806176448`
- 159796 sell 500 at 14:19:16
- 159796 buy 800 at 14:31:13

## Data source limitation

- The current minute-market-data endpoint/task did not return 2026-08-04 data; code must therefore retain a transaction-price fallback.

## Data correction completed

- Corrected the four persisted midnight timestamps for IDs `1785806176448` through `1785806176451` in PostgreSQL. The API now has chronological same-day trade times for all six transactions.

## New source-selection requirement

- `TencentPortfolioRealtimePriceGateway` is the existing live quote source and must remain the source during the trading session.
- `loadPortfolioIntradayPriceSeries` currently queries ClickHouse minute data on every request, including during market hours. It should query 1m only at/after 15:00 Shanghai time, after which the latest 1m point should be allowed to determine the final mark when present.
- Realtime/previous-close/date lookups currently receive only positive ledger positions. A symbol fully sold today can therefore miss the live quote needed for its same-day P&L; the quote universe must include every active-trade symbol, using a positive synthetic quantity for gateway lookup.
