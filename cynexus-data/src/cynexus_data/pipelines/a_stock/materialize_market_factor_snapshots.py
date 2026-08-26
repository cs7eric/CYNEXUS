from __future__ import annotations

from collections import Counter
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_stock_screening_facts import _requested_range
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import (
    count_stock_screening_fact_source_rows,
    get_latest_table_trade_date,
    read_stock_screening_fact_rows,
)
from cynexus_data.storage.postgres.market_factor_snapshot import MarketFactorSnapshotRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    start_date, end_date = _requested_range(request.params)
    result: dict[str, Any] = {"pipeline": "a_stock.materialize_market_factor_snapshots", "dry_run": request.dry_run, "rows_read": 0, "rows_written": 0, "warnings": []}
    if request.dry_run:
        return {**result, "notes": ["dry_run skips ClickHouse and PostgreSQL access"], "feature_version": MarketFactorSnapshotRepository.VERSION}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    clickhouse = context.hooks.clickhouse.get_client()
    if end_date is None:
        end_date = get_latest_table_trade_date(clickhouse, table="daily_raw")
        start_date = end_date
    if start_date is None or end_date is None:
        return {**result, "success": False, "error": "daily_raw has no available trade date"}
    rows = read_stock_screening_fact_rows(clickhouse, start_date=start_date, end_date=end_date, table="daily_raw")
    if not rows:
        return {**result, "success": False, "error": "no A-share daily_raw rows found"}
    repository = MarketFactorSnapshotRepository(context.hooks.postgres.get_client())
    batch_size = max(1, int(request.params.get("batch_size") or 2_000))
    written = repository.replace_range(
        rows,
        start_date=start_date.isoformat(),
        end_date=end_date.isoformat(),
        batch_size=batch_size,
    )
    coverage = Counter(str(row["trade_date"])[:10] for row in rows)
    expected_rows = count_stock_screening_fact_source_rows(clickhouse, start_date=start_date, end_date=end_date, table="daily_raw")
    materialized_rows = repository.count_rows(start_date=start_date.isoformat(), end_date=end_date.isoformat())
    coverage_pct = round(materialized_rows * 100 / expected_rows, 2) if expected_rows else 0.0
    if materialized_rows != expected_rows:
        result["warnings"].append(
            f"market-factor snapshots {materialized_rows}/{expected_rows} do not match daily_raw source coverage"
        )
    checkpoint = {"status": "completed", "feature_version": MarketFactorSnapshotRepository.VERSION,
                  "start_date": start_date.isoformat(), "end_date": end_date.isoformat(), "rows_read": len(rows),
                  "rows_written": written, "batch_size": batch_size, "coverage_by_trade_date": dict(coverage),
                  "source_daily_rows": expected_rows, "materialized_daily_rows": materialized_rows,
                  "coverage_pct": coverage_pct}
    context.checkpoints.put(str(request.params.get("checkpoint_key") or "a_stock.market_factor_snapshot.watermark"), request.job_key, checkpoint)
    if materialized_rows != expected_rows:
        return {**result, "rows_read": len(rows), "rows_written": written, "watermark": checkpoint,
                "success": False, "error": "market-factor snapshot coverage does not match its daily_raw source"}
    return {**result, "rows_read": len(rows), "rows_written": written, "watermark": checkpoint}
