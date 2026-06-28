# pipelines/market_overview

Market overview pipelines maintain lightweight daily market snapshots for Java/UI and downstream factors.

- `build_market_overview.py` merges breadth/fund-flow fields into the daily serving table.
- `build_market_overview_detail.py` reads compact ClickHouse daily bars, calls pure market technical helpers, and writes one UI-ready JSON detail payload to PostgreSQL.

The detail pipeline must not copy OHLCV history into PostgreSQL; only the final compact payload belongs there.
