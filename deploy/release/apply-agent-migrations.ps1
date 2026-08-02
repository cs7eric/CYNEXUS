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
    "20260722_add_ai_agent_quota_policy.sql",
    "20260722_add_ai_agent_feedback.sql",
    "20260722_add_ai_agent_write_commands.sql",
    "20260722_bind_autonomous_agent_to_xtyooo.sql",
    "20260722_add_ai_agent_tasks.sql",
    "20260722_add_ai_agent_task_notifications.sql",
    "20260722_add_ai_agent_capabilities.sql",
    "20260723_add_ai_agent_dynamic_capabilities.sql",
    "20260722_repair_ai_agent_capability_runtime_state.sql",
    "20260722_harden_ai_agent_owner_constraints.sql",
    "20260724_seed_autonomous_agent_prompt.sql",
    "20260725_fix_autonomous_agent_markdown_prompt.sql",
    "20260726_optimize_autonomous_agent_prompt_cache_prefix.sql",
    "20260731_update_autonomous_agent_prompt_v4.sql",
    "20260726_route_autonomous_agent_via_responses.sql",
    "20260726_enable_xhhai_responses_websocket.sql",
    "20260726_use_buffered_xhhai_responses_http.sql",
    "20260725_add_ai_agent_provider_selection.sql",
    "20260725_add_ai_provider_model_configs.sql",
    "20260725_add_ai_binding_reasoning_effort.sql",
    "20260725_add_ai_agent_capability_guidance.sql",
    "20260725_fix_xhhai_provider_models.sql",
    "20260726_add_ai_agent_prompt_cache_usage.sql",
    "20260726_add_ai_agent_response_state_and_cache_telemetry.sql",
    "20260726_add_agent_stateless_responses_replay.sql",
    "20260726_seed_agent_prompt_cache_architecture_kb.sql",
    "20260726_update_agent_prompt_cache_architecture_kb_zh.sql",
    "20260726_update_agent_prompt_cache_architecture_kb_v3_zh.sql"
    "20260726_update_agent_prompt_cache_architecture_kb_v4_zh.sql"
    "20260726_update_agent_prompt_cache_architecture_kb_v5_zh.sql"
    "20260726_update_agent_prompt_cache_architecture_kb_v6_zh.sql"
    "20260727_update_agent_prompt_cache_architecture_kb_v7_zh.sql"
    "20260727_update_agent_prompt_cache_architecture_kb_v8_zh.sql"
    "20260727_update_agent_prompt_cache_architecture_kb_v9_zh.sql"
    "20260727_enable_xhhai_responses_sse.sql"
    "20260727_add_ai_agent_run_reasoning_effort.sql"
    "20260801_seed_futures_position_ai_analysis_prompt.sql"
    ,"20260801_seed_portfolio_trade_screenshot_prompt.sql"
    ,"20260802_normalize_ai_capability_labels.sql"
    ,"20260802_refine_portfolio_trade_screenshot_prompt.sql"
    ,"20260802_refine_portfolio_trade_screenshot_prompt_v3.sql"
    ,"20260802_refine_portfolio_trade_screenshot_prompt_v4.sql"
)

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing Agent migration: $file" }
    Write-Host "[cynexus] Applying Agent migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Agent migration failed: $file" }
}

Write-Host "[cynexus] Agent migrations are applied."
