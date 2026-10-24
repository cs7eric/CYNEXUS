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

foreach ($file in @(
    "20261019_01_create_food_nutrition_catalog.sql",
    "20261019_02_create_food_diary.sql",
    "20261019_03_create_food_fridge.sql",
    "20261019_05_create_food_asset_mapping.sql",
    "20261020_02_extend_food_asset_mapping.sql",
    "20261020_01_create_workout.sql",
    "20261020_03_create_workout_request_ledger.sql",
    "20261021_02_auto_match_fridge_nutrition.sql",
    "20261021_03_backfill_food_asset_mapping.sql",
    "20261023_01_map_food_assets_to_catalog.sql",
    "20261024_01_create_food_custom_asset.sql",
    "20261024_04_create_nutrition_configuration.sql"
)) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing Life migration: $file" }
    Write-Host "[cynexus] Applying Life migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -X -U $User -d $Database -v ON_ERROR_STOP=1 -1
    if ($LASTEXITCODE -ne 0) { throw "Life migration failed: $file" }
}

Write-Host "[cynexus] Life migrations are applied."
