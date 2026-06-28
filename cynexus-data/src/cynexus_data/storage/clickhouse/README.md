# storage/clickhouse

ClickHouse storage layer.

Current code:

- `writers.py`: current 1m K-line writer and table creation helper.

Future files:

- `client.py`: thin client wrapper from a hook-provided connection.
- `readers.py`: query helpers.
- `ddl.py`: DDL helpers.
- `mutations.py`: repair, delete, replace, and recompute operations.

