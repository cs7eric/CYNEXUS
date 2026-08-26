from __future__ import annotations

from collections import Counter
from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import (
    count_stock_screening_fact_source_rows,
    get_latest_table_trade_date,
    read_stock_screening_fact_rows,
)
from cynexus_data.storage.postgres.stock_screening_fact import StockScreeningFactRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    """Materialize the durable, replayable daily Screener projection.

    This projection is intentionally source-aligned with ClickHouse `daily_raw`.
    It does not claim to include fund flow, industry, or financial coverage;
    those remain independently versioned sources in the serving Screener.
    """
    start_date, end_date = _requested_range(request.params)
    result: dict[str, Any] = {
        "pipeline": "a_stock.materialize_stock_screening_facts",
        "dry_run": request.dry_run,
        "source": StockScreeningFactRepository.SOURCE,
        "start_date": start_date.isoformat() if start_date else None,
        "end_date": end_date.isoformat() if end_date else None,
        "rows_read": 0,
        "rows_written": 0,
        "coverage_by_trade_date": [],
        "warnings": [],
    }
    if request.dry_run:
        result["notes"] = [
            "dry_run skips ClickHouse and PostgreSQL access; pass trade_date or start_date/end_date to make the target range explicit"
        ]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    clickhouse = context.hooks.clickhouse.get_client()
    if end_date is None:
        end_date = get_latest_table_trade_date(clickhouse, table=str(request.params.get("source_table") or "daily_raw"))
        start_date = end_date
    if start_date is None or end_date is None:
        return {**result, "success": False, "error": "daily_raw has no available trade date"}
    rows = read_stock_screening_fact_rows(
        clickhouse,
        start_date=start_date,
        end_date=end_date,
        table=str(request.params.get("source_table") or "daily_raw"),
    )
    coverage = Counter(str(row["trade_date"])[:10] for row in rows)
    result.update(
        {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "rows_read": len(rows),
            "coverage_by_trade_date": [
                {"trade_date": trade_date, "source_daily_rows": count}
                for trade_date, count in sorted(coverage.items())
            ],
        }
    )
    if not rows:
        return {**result, "success": False, "error": "no A-share daily_raw rows found for requested range"}
    repository = StockScreeningFactRepository(context.hooks.postgres.get_client())
    batch_size = max(1, int(request.params.get("batch_size") or 2_000))
    written = repository.replace_range(
        rows,
        start_date=start_date,
        end_date=end_date,
        batch_size=batch_size,
    )
    source_table = str(request.params.get("source_table") or "daily_raw")
    expected_rows = count_stock_screening_fact_source_rows(
        clickhouse,
        start_date=start_date,
        end_date=end_date,
        table=source_table,
    )
    materialized_rows = repository.count_rows(start_date=start_date, end_date=end_date)
    coverage_pct = round(materialized_rows * 100 / expected_rows, 2) if expected_rows else 0.0
    if materialized_rows != expected_rows:
        result["warnings"].append(
            f"materialized screening facts {materialized_rows}/{expected_rows} do not match the daily_raw source coverage"
        )
    checkpoint = {
        "status": "completed",
        "source": StockScreeningFactRepository.SOURCE,
        "start_date": start_date.isoformat(),
        "end_date": end_date.isoformat(),
        "rows_read": len(rows),
        "rows_written": written,
        "coverage_by_trade_date": result["coverage_by_trade_date"],
        "source_daily_rows": expected_rows,
        "materialized_daily_rows": materialized_rows,
        "coverage_pct": coverage_pct,
    }
    context.checkpoints.put(
        str(request.params.get("checkpoint_key") or "a_stock.stock_screening_fact.watermark"),
        request.job_key,
        checkpoint,
    )
    if materialized_rows != expected_rows:
        return {
            **result,
            "rows_written": written,
            "watermark": checkpoint,
            "success": False,
            "error": "screening fact coverage does not match its daily_raw source",
        }
    return {**result, "rows_written": written, "watermark": checkpoint}


def _requested_range(params: dict[str, Any]) -> tuple[date | None, date | None]:
    start = _parse_date(params.get("start_date") or params.get("trade_date"))
    end = _parse_date(params.get("end_date") or params.get("trade_date"))
    if start and not end:
        end = start
    if end and not start:
        start = end
    if start and end and start > end:
        raise ValueError("start_date must not be after end_date")
    return start, end


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])
