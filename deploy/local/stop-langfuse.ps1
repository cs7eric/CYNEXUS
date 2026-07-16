$ErrorActionPreference = "Stop"
docker compose -f (Join-Path $PSScriptRoot "docker-compose.langfuse.yml") down
