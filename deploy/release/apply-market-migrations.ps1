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
$tradeReviewStateDdl = Join-Path $repoRoot "cynexus-data\sql\postgres\20260811_create_trade_review_market_state.sql"

$migrationFiles = @(
    "20260730_create_futures_position_tables.sql",
    "20260731_fix_futures_position_id_sequence.sql",
    "20260730_schedule_futures_position_daily.sql",
    "20260731_enable_futures_sina_fallback.sql",
    "20260801_optimize_futures_position_history.sql",
    "20260804_use_latest_futures_position_source_date.sql",
    "20260728_schedule_margin_stock_history_backfill_batch.sql",
    "20260727_create_market_indicator_tables.sql",
    "20260728_create_market_indicator_industry_tables.sql",
    "20260727_schedule_market_indicator_refresh.sql",
    "20260728_create_stock_screening_tables.sql",
    "20260728_schedule_market_indicator_refresh.sql",
    "20260805_create_market_indicator_transmission_tables.sql",
    "20260805_create_market_indicator_benchmark.sql",
    "20260805_allow_market_indicator_watchlist_kind.sql",
    "20260805_schedule_market_indicator_refresh.sql",
    "20260805_schedule_market_indicator_review.sql",
    "20260729_normalize_scheduler_job_catalog.sql",
    "20260805_schedule_market_indicator_priority_refresh.sql",
    "20260805_tune_market_indicator_quote_batches.sql",
    "20260811_create_market_indicator_limit_board_snapshots.sql",
    "20260811_reduce_market_indicator_limit_board_to_best_quotes.sql",
    "20260811_create_market_indicator_limit_board_session_summaries.sql",
    "20260811_schedule_market_indicator_limit_board.sql",
    "20260806_schedule_social_scheduler_jobs.sql",
    "20260806_make_1m_tdx_sync_incremental.sql",
    "20260806_extend_1m_tdx_incremental_window.sql",
    "20260806_resume_1m_tdx_from_effective_date.sql",
    "20260801_create_personal_portfolio_tables.sql",
    "20260802_add_portfolio_instrument_type.sql",
    "20260802_add_portfolio_process_history.sql",
    "20260802_backfill_portfolio_candidate_payload.sql",
    "20260803_aggregate_portfolio_process_history.sql",
    "20260803_reset_personal_portfolio_realized_pnl.sql",
    "20260806_fix_portfolio_pnl_baseline_reset_at.sql",
    "20260803_create_portfolio_account.sql",
    "20260803_backfill_portfolio_trading_fees.sql",
    "20260804_change_portfolio_account_total_assets.sql",
    "20260804_prevent_portfolio_trade_duplicates.sql",
    "20260805_backfill_portfolio_trade_times.sql"
    ,"20260807_repair_portfolio_ai_fees.sql"
    ,"20260807_create_portfolio_review_anchors.sql"
    ,"20260809_create_trade_review_timeshare_archive.sql"
    ,"20260809_schedule_trade_review_timeshare.sql"
    ,"20260810_fix_portfolio_trade_times_and_import_date.sql"
    ,"20260810_repair_portfolio_trade_wall_clock_times.sql"
    ,"20260810_disable_1m_kline_delete_mutations.sql"
    ,"20260811_schedule_trade_review_market_state.sql"
    ,"20260812_add_trade_review_market_minute_metrics.sql"
    ,"20260813_repair_limit_board_session_duration.sql"
    ,"20260813_limit_board_session_capture.sql"
    ,"20260814_limit_board_sealed_turnover_ratio.sql"
    ,"20260815_create_stock_selection_strategy_tables.sql"
    ,"20260815_create_stock_selection_backtest_tables.sql"
    ,"20260815_extend_stock_selection_backtest_status.sql"
    ,"20260815_optimize_market_indicator_candidate_capture.sql"
    ,"20260816_schedule_stock_screening_fact_materialization.sql"
    ,"20260816_extend_stock_selection_user_strategies.sql"
    ,"20260816_create_stock_selection_named_watchlists.sql"
    ,"20260816_create_market_factor_research.sql"
    ,"20260817_add_market_factor_study_outcomes.sql"
    ,"20260819_optimize_market_factor_study_snapshot_lookup.sql"
    ,"20260826_clear_portfolio_retry_errors_after_success.sql"
)

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing market migration: $file" }
    Write-Host "[cynexus] Applying market migration: $file"
    Get-Content -LiteralPath $path -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Market migration failed: $file" }
}

if (-not (Test-Path -LiteralPath $tradeReviewStateDdl)) { throw "Missing Trade Review market-state DDL: $tradeReviewStateDdl" }
Write-Host "[cynexus] Applying Trade Review market-state DDL"
Get-Content -LiteralPath $tradeReviewStateDdl -Encoding utf8 -Raw | docker exec -i $PostgresContainer psql -U $User -d $Database -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw "Trade Review market-state DDL failed" }

Write-Host "[cynexus] Market migrations are applied."
