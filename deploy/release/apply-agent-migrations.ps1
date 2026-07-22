param(
    [string] $PostgresContainer = "cynexus-postgres",
    [string] $Database = "cynexus",
    [string] $User = "postgres"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$migrationDir = Join-Path $repoRoot "deploy\local\postgres\migrations"

$migrationFiles = @(
    "20260722_create_ai_agent_tables.sql",
    "20260722_create_ai_agent_messages.sql",
    "20260722_add_ai_agent_idempotency_and_retry_fields.sql",
    "20260722_add_ai_agent_run_evidence_fields.sql",
    "20260722_add_ai_agent_clarification.sql",
    "20260722_add_ai_agent_event_log.sql",
    "20260722_add_ai_agent_failure_categories.sql",
    "20260722_add_ai_agent_cancellation_intent.sql",
    "20260722_add_ai_agent_citations.sql",
    "20260722_add_ai_agent_source_urls.sql",
    "20260722_add_ai_agent_usage_quota.sql",
    "20260722_add_ai_agent_feedback.sql",
    "20260722_add_ai_agent_write_commands.sql",
    "20260722_bind_autonomous_agent_to_xtyooo.sql",
    "20260722_add_ai_agent_tasks.sql",
    "20260722_add_ai_agent_capabilities.sql",
    "20260722_repair_ai_agent_capability_runtime_state.sql"
)

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing Agent migration: $file" }
    Write-Host "[cynexus] Applying Agent migration: $file"
    Get-Content -LiteralPath $path -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Agent migration failed: $file" }
}

Write-Host "[cynexus] Agent migrations are applied."
