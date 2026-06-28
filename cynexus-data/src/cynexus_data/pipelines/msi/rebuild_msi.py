from __future__ import annotations

from datetime import date, timedelta
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.pipelines.msi import build_msi


def run(context: PipelineContext, request: JobRequest) -> dict:
    end_date = _parse_date(request.params.get("end_date")) or _parse_date(request.params.get("trade_date")) or date.today()
    start_date = _parse_date(request.params.get("start_date")) or end_date
    if start_date > end_date:
        raise ValueError("start_date must be <= end_date")
    dates = _date_range(start_date, end_date)
    result = {
        "pipeline": "msi.rebuild_msi",
        "dry_run": request.dry_run,
        "start_date": start_date.isoformat(),
        "end_date": end_date.isoformat(),
        "planned_dates": len(dates),
        "rows_read": 0,
        "rows_written": 0,
        "date_results": [],
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped per-date build_msi execution"]
        return result
    for current in dates:
        child_params = dict(request.params)
        child_params["trade_date"] = current.isoformat()
        child_request = JobRequest(job_key="build_msi", dry_run=False, params=child_params)
        payload = build_msi.run(context, child_request)
        result["rows_read"] += int(payload.get("rows_read") or 0)
        result["rows_written"] += int(payload.get("rows_written") or 0)
        result["date_results"].append(
            {
                "trade_date": current.isoformat(),
                "rows_read": payload.get("rows_read"),
                "rows_written": payload.get("rows_written"),
                "skipped": payload.get("skipped", False),
                "from_cache": payload.get("from_cache", False),
            }
        )
    result["date_results_truncated"] = len(result["date_results"]) > 50
    result["date_results"] = result["date_results"][:50]
    return result


def _date_range(start: date, end: date) -> list[date]:
    current = start
    out: list[date] = []
    while current <= end:
        out.append(current)
        current += timedelta(days=1)
    return out


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))
