# Progress Log

## 2026-07-27

- Read project `AGENTS.md`, product/design context, and required frontend skill guidance.
- Inspected PyPI and the `electkismet/eltdx` source/docs at v1.2.0.
- Confirmed the release data executor already has `eltdx 1.2.0` installed.
- Confirmed current repository adapters and the existing 1m/scheduler architecture.
- Implemented the market-indicator workflow across Python, PostgreSQL, Java, and UI.
- Added eltdx 1.2.x quote collection, normalization, rapid-move and limit-state event detection, and the `market_indicator_refresh` job.
- Added `market_indicator_snapshots` and `market_indicator_events` under `cynexus_appl_market`, with scheduler configuration for 10-minute trading-session refreshes.
- Added Java `GET /api/market/indicator/latest` and the `/market/indicator` UI route with snapshot and event views.
- Fixed ratio-to-percentage conversion for `sec_stock_basic.limit_up_threshold`; focused tests now pass 4/4.
- Synchronized versions to `1.1.307-SNAPSHOT`, rebuilt the data executor and Release images, and verified the Release stack and endpoint.

## 2026-07-28

- Added breadth counts for advancing/declining, limit-up/limit-down, and rapid moves.
- Added limit-board lists with stock name, code, and industry, plus industry-level counts and first observed board/event fields.
- Added persisted industry snapshots and Java/UI exposure under `/api/market/indicator/latest` and `/market/indicator`.
- Corrected latest-snapshot filtering so status filters apply after the latest quote per stock is selected; retained first observed industry events across captures.
- Corrected the MyBatis XML comparison escape and included `last_price` in the latest snapshot projection.
- Ran the indicator job successfully (`19` snapshots, `9` industry rows, `1` event), with Release API returning breadth, industries, names, and prices.
- Synchronized versions to `1.1.310-SNAPSHOT`, rebuilt the data executor and Release images, and verified the Release stack and API/UI smoke checks.
