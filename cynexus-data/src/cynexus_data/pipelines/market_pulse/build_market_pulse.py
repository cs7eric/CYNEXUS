from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.market_pulse import build_market_pulse_rows, build_strong_and_flow_payload
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.fund_flow import SectorFundFlowRepository
from cynexus_data.storage.postgres.market_pulse import MarketPulseRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    create_table = _truthy(request.params.get("create_table"))
    manual_rows = _parse_rows(request.params.get("rows"))
    result = {
        "pipeline": "market_pulse.build_market_pulse",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        rows = manual_rows or []
        pulse_rows = build_market_pulse_rows(rows, trade_date=trade_date)
        result["rows_read"] = len(rows)
        result["payload"] = build_strong_and_flow_payload(rows, trade_date=trade_date)
        result["planned_rows"] = len(pulse_rows)
        result["notes"] = ["dry_run skipped Postgres access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    repo = MarketPulseRepository(pg)
    if create_table:
        repo.create_table()
    raw_rows = manual_rows if manual_rows is not None else SectorFundFlowRepository(pg).get_daily_snapshot(trade_date)
    pulse_rows = build_market_pulse_rows(raw_rows, trade_date=trade_date)
    if not pulse_rows:
        return {**result, "skipped": True, "skip_reason": "no_sector_fund_flow_rows"}
    result["rows_read"] = len(raw_rows)
    result["rows_written"] = repo.replace_snapshot(trade_date=trade_date, rows=pulse_rows)
    result["payload"] = build_strong_and_flow_payload(pulse_rows, trade_date=trade_date)
    return result


def _parse_rows(value: Any) -> list[dict[str, Any]] | None:
    if value in (None, ""):
        return None
    if isinstance(value, list):
        return [row for row in value if isinstance(row, dict)]
    parsed = json.loads(str(value))
    if not isinstance(parsed, list):
        raise ValueError("rows must be a JSON list")
    return [row for row in parsed if isinstance(row, dict)]


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
