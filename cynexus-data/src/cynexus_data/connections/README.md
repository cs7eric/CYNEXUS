# connections

Connection configuration layer.

This layer describes external systems by `conn_id`: ClickHouse, Postgres, EastMoney, TDX local filesystem, DuckDB, Redis, and future services. It should validate and resolve configuration, but it should not create business clients or fetch market data.

Expected files from `INFRA.md`:

- `models.py`
- `registry.py`
- `resolver.py`
- `secrets.py`
- `factory.py`
- `validators.py`

