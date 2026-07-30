param(
    [string] $PostgresContainer = "cynexus-postgres",
    [string] $Database = "cynexus",
    [string] $User = "postgres"
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$migrationDir = Join-Path $repoRoot "deploy\local\postgres\migrations"

$migrationFiles = @(
    "20260730_create_futures_position_tables.sql",
    "20260730_schedule_futures_position_daily.sql",
    "20260728_schedule_margin_stock_history_backfill_batch.sql",
    "20260727_create_market_indicator_tables.sql",
    "20260728_create_market_indicator_industry_tables.sql",
    "20260727_schedule_market_indicator_refresh.sql",
    "20260728_create_stock_screening_tables.sql",
    "20260728_schedule_market_indicator_refresh.sql",
    "20260729_normalize_scheduler_job_catalog.sql"
)

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing market migration: $file" }
    Write-Host "[cynexus] Applying market migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Market migration failed: $file" }
}

Write-Host "[cynexus] Market migrations are applied."
