from __future__ import annotations

from datetime import date, timedelta
from typing import Any

from cynexus_data.core.calendar import generate_trade_calendar
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.trade_calendar import TradeCalendarRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    start = _parse_date(request.params.get("start_date")) or (date.today() - timedelta(days=30))
    end = _parse_date(request.params.get("end_date")) or (date.today() + timedelta(days=365))
    extra_holidays = _parse_dates(request.params.get("extra_holidays"))
    extra_workdays = _parse_dates(request.params.get("extra_workdays"))
    records = generate_trade_calendar(start, end, extra_holidays=extra_holidays, extra_workdays=extra_workdays)
    result = {
        "pipeline": "stock_basic.sync_trade_calendar",
        "dry_run": request.dry_run,
        "start_date": start.isoformat(),
        "end_date": end.isoformat(),
        "rows_read": len(records),
        "rows_written": 0,
        "trading_days": sum(1 for row in records if row["is_trading_day"]),
    }
    if request.dry_run:
        return {**result, "sample": _sample(records), "notes": ["dry_run skipped Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = TradeCalendarRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()
    result["rows_written"] = repo.upsert_many(records, source=str(request.params.get("source") or "local_rules"))
    return result


def _sample(records: list[dict[str, object]]) -> list[dict[str, object]]:
    return [{**row, "trade_date": row["trade_date"].isoformat()} for row in records[:5]]


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _parse_dates(value: Any) -> list[date]:
    if value in (None, ""):
        return []
    if isinstance(value, (list, tuple, set)):
        return [_parse_date(item) for item in value if _parse_date(item) is not None]
    return [_parse_date(item.strip()) for item in str(value).split(",") if item.strip()]


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
