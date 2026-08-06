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

foreach ($file in @("20260803_create_wechat_event_history.sql")) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing WeChat migration: $file" }
    Write-Host "[cynexus] Applying WeChat migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "WeChat migration failed: $file" }
}

Write-Host "[cynexus] WeChat migrations are applied."
