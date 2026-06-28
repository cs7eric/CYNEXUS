from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.domain.market.msi import sector_breadth_score
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.msi import MsiRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    create_table = _truthy(request.params.get("create_table"))
    result = {
        "pipeline": "msi.build_sector_breadth",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
    }
    manual = _manual_payload(request.params, trade_date)
    if request.dry_run:
        result["notes"] = ["dry_run skipped Postgres access"]
        if manual is not None:
            result["rows_read"] = int(manual["total"])
            result["payload"] = manual
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = MsiRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    payload = manual or repo.compute_sector_breadth_from_fund_flow(trade_date)
    if payload is None:
        return {**result, "skipped": True, "skip_reason": "no_sector_fund_flow_snapshot"}
    result["rows_read"] = int(payload.get("total") or 0)
    result["rows_written"] = repo.save_sector_breadth(payload)
    result["payload"] = payload
    return result


def _manual_payload(params: dict[str, Any], trade_date: date) -> dict[str, Any] | None:
    if "advancing" not in params or "total" not in params:
        return None
    advancing = int(params.get("advancing") or 0)
    declining = int(params.get("declining") or 0)
    flat = int(params.get("flat") or 0)
    total = int(params.get("total") or 0)
    score = sector_breadth_score(advancing=advancing, total=total)
    return {
        "tradeDate": trade_date.isoformat(),
        "advancing": advancing,
        "declining": declining,
        "flat": flat,
        "total": total,
        "advancePct": round(advancing / total, 6) if total else None,
        "score": score,
        "source": "manual_params",
    }


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
