$ErrorActionPreference = "Stop"

$compose = Join-Path $PSScriptRoot "docker-compose.langfuse.yml"
$postgresMigration = Join-Path $PSScriptRoot "postgres\migrations\20260715_create_langfuse_schema.sql"
$clickhouseMigration = Join-Path $PSScriptRoot "clickhouse\migrations\20260715_create_langfuse_database.sql"

docker network inspect cynexus-dev_default *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Shared Cynexus network cynexus-dev_default is not running. Start deploy/local/docker-compose.yml first."
}

Get-Content -Raw $postgresMigration | docker exec -i cynexus-postgres psql -U postgres -d cynexus
if ($LASTEXITCODE -ne 0) { throw "Could not initialize the Langfuse PostgreSQL schema." }
Get-Content -Raw $clickhouseMigration | docker exec -i cynexus-clickhouse clickhouse-client --user cynexus --password C020611. --multiquery
if ($LASTEXITCODE -ne 0) { throw "Could not initialize the Langfuse ClickHouse database." }

docker compose -f $compose up -d
