param(
    [string[]] $Service = @("core", "market", "ai", "gateway"),
    [switch] $NoCache,
    [switch] $SkipDataJobExecutor
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$composeFile = Join-Path $scriptDir "docker-compose.yml"

Set-Location $repoRoot

Write-Host "[cynexus] Using compose file: $composeFile"
Write-Host "[cynexus] Packaging current source with Maven"
mvn clean package -DskipTests

Write-Host "[cynexus] Rebuilding services from current source: $($Service -join ', ')"

if ($NoCache) {
    docker compose -f $composeFile build --no-cache @Service
    docker compose -f $composeFile up -d --force-recreate @Service
} else {
    docker compose -f $composeFile up -d --build --force-recreate @Service
}

Write-Host ""
Write-Host "[cynexus] Services are starting. Useful checks:"
Write-Host "  curl http://127.0.0.1:18080/actuator/health"
Write-Host "  curl http://127.0.0.1:18081/actuator/health"
Write-Host "  curl http://127.0.0.1:18082/actuator/health"
Write-Host "  curl http://127.0.0.1:18000/actuator/health"
Write-Host "  curl http://127.0.0.1:18000/api/core/info"

if (-not $SkipDataJobExecutor) {
    & (Join-Path $scriptDir "start-data-job-executor.cmd")
    Write-Host "  curl http://127.0.0.1:18083/internal/health"
}
