$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptDir "docker-compose.yml"
docker compose -f $composeFile down
Write-Host "Cynexus release Docker stack stopped."
