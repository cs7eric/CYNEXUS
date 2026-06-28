# storage/clickhouse

ClickHouse storage layer.

Current code:

- `client.py`: thin client wrapper from a hook-provided connection.
- `readers.py`: query helpers for minute/daily K-line quality, daily adjusted/raw bars, index daily bars, MSI inputs, market overview detail inputs, and aggregation counts.
- `writers.py`: current K-line and intraday-detail writers plus table creation helpers.
- `ddl.py`: DDL helpers.
- `mutations.py`: repair, delete, replace, and recompute operations.
