# Local Langfuse

Langfuse is optional and uses the existing `cynexus-postgres`,
`cynexus-clickhouse` and `cynexus-redis` containers. It does not create a
second database stack. Start the shared infrastructure first, then create the
isolated PostgreSQL schema and ClickHouse database:

```powershell
Get-Content -Raw deploy/local/postgres/migrations/20260715_create_langfuse_schema.sql |
  docker exec -i cynexus-postgres psql -U postgres -d cynexus
Get-Content -Raw deploy/local/clickhouse/migrations/20260715_create_langfuse_database.sql |
  docker exec -i cynexus-clickhouse clickhouse-client --user cynexus --password C020611. --multiquery
docker compose -f deploy/local/docker-compose.langfuse.yml up -d
```

The same operation is available as `deploy/local/start-langfuse.ps1`; stop it
with `deploy/local/stop-langfuse.ps1`. These scripts only touch Langfuse and
never stop the shared data containers.

The MinIO entrypoint creates the `langfuse` bucket directory before starting,
so no additional object-storage service or bootstrap container is required.

Set `CYNEXUS_AI_LANGFUSE_ENABLED=true`, `CYNEXUS_AI_LANGFUSE_PUBLIC_KEY` and
`CYNEXUS_AI_LANGFUSE_SECRET_KEY` for the AI service. The browser uses
`/api/ai/usage`; Langfuse credentials never leave the backend.
