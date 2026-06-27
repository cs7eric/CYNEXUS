# Cynexus Local Docker

Use `up-latest.ps1` when you want Docker services to run the newest source code.

```powershell
.\deploy\local\up-latest.ps1
```

It runs `mvn clean package -DskipTests`, then rebuilds and recreates `core`, `market`, `ai`, and `gateway` from the current workspace.

The local gateway is the single backend entrypoint:

```powershell
curl http://127.0.0.1:18000/api/core/info
curl http://127.0.0.1:18000/api/ai/config/providers
curl http://127.0.0.1:18000/api/market/data/overview-snapshots
```

For a full rebuild without Docker layer cache:

```powershell
.\deploy\local\up-latest.ps1 -NoCache
```

ClickHouse is available as local analytical storage for DuckDB migration tests:

```powershell
docker compose -f deploy/local/docker-compose.yml up -d clickhouse
docker exec cynexus-clickhouse clickhouse-client --user cynexus --password C020611. --query "SELECT version()"
```

Local ports:

- HTTP: `127.0.0.1:28123`
- Native client: `127.0.0.1:29000`
- Database: `cynexus`
- User: `cynexus`

ClickHouse uses Docker named volumes `clickhouse-data` and `clickhouse-logs`.
On Windows, a bind-mounted ClickHouse data directory can fail on MergeTree
atomic renames during inserts, so it is intentionally not mounted like Redis.

DuckDB market detail tables can be loaded into ClickHouse with:

```powershell
Get-Content -Raw deploy\local\clickhouse\migrations\20260626_create_duckdb_market_tables.sql |
  docker exec -i cynexus-clickhouse clickhouse-client --user cynexus --password C020611. --database cynexus --multiquery

python deploy\local\clickhouse\scripts\migrate_duckdb_to_clickhouse.py --replace
```
