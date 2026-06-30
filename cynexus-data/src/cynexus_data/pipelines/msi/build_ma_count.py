from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.ma_count import calculate_ma_count_summary
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_ma_count_input_rows
from cynexus_data.storage.postgres.msi import MsiRepository
from cynexus_data.storage.postgres.percentile import PercentileRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    create_table = _truthy(request.params.get("create_table"))
    rows = _parse_rows(request.params.get("rows"))
    result = {
        "pipeline": "msi.build_ma_count",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        input_rows = rows or []
        payload = calculate_ma_count_summary(input_rows, trade_date=trade_date)
        return {**result, "rows_read": len(input_rows), "payload": payload, "notes": ["dry_run skipped ClickHouse and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    input_rows = rows if rows is not None else read_ma_count_input_rows(context.hooks.clickhouse.get_client(), trade_date, table=str(request.params.get("source_table") or "daily_qfq"))
    payload = calculate_ma_count_summary(input_rows, trade_date=trade_date)
    pg_client = context.hooks.postgres.get_client()
    _apply_history_scores(payload, PercentileRepository(pg_client), trade_date)
    repo = MsiRepository(pg_client)
    if create_table:
        repo.create_table()
    result["rows_read"] = len(input_rows)
    result["rows_written"] = repo.save_ma_count(payload)
    result["payload"] = payload
    return result


def _apply_history_scores(payload: dict[str, Any], percentile_repo: PercentileRepository, trade_date: date) -> None:
    if payload.get("pctNewHigh252d") is not None:
        payload["priceStrengthScore"] = percentile_repo.percentile_score("ma_count_daily", "new_high_252d_pct", trade_date, payload.get("pctNewHigh252d"))
    if payload.get("breadthRaw") is not None:
        payload["breadthScore"] = percentile_repo.percentile_score("ma_count_daily", "breadth_raw", trade_date, payload.get("breadthRaw"))


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
