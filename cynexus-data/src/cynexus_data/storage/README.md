# storage

Storage access layer.

Storage modules read and write tables. They should not fetch third-party market data or implement domain calculations.

Use:

- `clickhouse/` for large time-series facts and analytical scans.
- `postgres/` for business metadata, task state, UI snapshots, and serving results.
- `duckdb/` for local migration or offline analytical files.

