from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.domain.market.ma_count import calculate_profit_effect
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.msi import MsiRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    create_table = _truthy(request.params.get("create_table"))
    up_5d_pct = request.params.get("up_5d_pct") or request.params.get("up5dPct")
    new_low_60d_pct = request.params.get("new_low_60d_pct") or request.params.get("newLow60dPct")
    result = {
        "pipeline": "msi.build_profit_effect",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
    }
    repo: MsiRepository | None = None
    if not request.dry_run:
        if context.hooks is None:
            raise RuntimeError("PipelineContext.hooks is required")
        repo = MsiRepository(context.hooks.postgres.get_client())
        if create_table:
            repo.create_table()
        if up_5d_pct is None or new_low_60d_pct is None:
            ma_payload = repo.get_ma_count(trade_date)
            if ma_payload is None:
                return {**result, "skipped": True, "skip_reason": "missing_msi_ma_count_daily"}
            up_5d_pct = ma_payload.get("pctUp5d")
            new_low_60d_pct = ma_payload.get("pctNewLow60d")
            result["rows_read"] = 1
    payload = calculate_profit_effect(trade_date=trade_date, up_5d_pct=up_5d_pct, new_low_60d_pct=new_low_60d_pct)
    result["payload"] = payload
    if repo is not None:
        result["rows_written"] = repo.save_profit_effect(payload)
    return result


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
