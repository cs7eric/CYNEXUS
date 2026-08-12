param(
    [string] $ClickHouseContainer = "cynexus-clickhouse",
    [string] $Database = "cynexus",
    [string] $User = "cynexus",
    [string] $Password = "C020611."
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$migrationDir = Join-Path $repoRoot "cynexus-data\sql\clickhouse"

$migrationFiles = @(
    "20260809_create_trade_review_timeshare.sql"
)

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing ClickHouse migration: $file" }
    Write-Host "[cynexus] Applying ClickHouse migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw |
        docker exec -i $ClickHouseContainer clickhouse-client --user $User --password $Password --database $Database --multiquery
    if ($LASTEXITCODE -ne 0) { throw "ClickHouse migration failed: $file" }
}

Write-Host "[cynexus] ClickHouse market migrations are applied."
