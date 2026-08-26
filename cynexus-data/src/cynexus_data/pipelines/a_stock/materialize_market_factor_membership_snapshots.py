from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.market_factor_membership_snapshot import MarketFactorMembershipSnapshotRepository
from cynexus_data.storage.postgres.ths_industry_constituents import ThsIndustryConstituentRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    trade_date = date.fromisoformat(str(request.params.get("trade_date"))) if request.params.get("trade_date") else date.today()
    result: dict[str, Any] = {"pipeline": "a_stock.materialize_market_factor_membership_snapshots", "trade_date": trade_date.isoformat(),
                              "dry_run": request.dry_run, "rows_read": 0, "rows_written": 0, "warnings": []}
    if request.dry_run:
        return {**result, "notes": ["dry_run skips PostgreSQL access"], "membership": "ths"}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    client = context.hooks.postgres.get_client()
    source_rows = ThsIndustryConstituentRepository(client).list_records_as_of(snapshot_date=trade_date)
    written = MarketFactorMembershipSnapshotRepository(client).replace_effective_date(effective_trade_date=trade_date, records=source_rows)
    coverage = round(written * 100 / len(source_rows), 2) if source_rows else 0.0
    checkpoint = {"status": "completed", "trade_date": trade_date.isoformat(), "membership": "ths", "rows_read": len(source_rows),
                  "rows_written": written, "coverage_pct": coverage}
    context.checkpoints.put(str(request.params.get("checkpoint_key") or "a_stock.market_factor_membership.watermark"), request.job_key, checkpoint)
    if written != len(source_rows):
        return {**result, "rows_read": len(source_rows), "rows_written": written, "watermark": checkpoint, "success": False,
                "error": "membership snapshot coverage does not match its source"}
    return {**result, "rows_read": len(source_rows), "rows_written": written, "watermark": checkpoint}
