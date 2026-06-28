from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.msi_factors import calculate_turnover_activity
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_turnover_activity_rows
from cynexus_data.storage.postgres.msi import MsiRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    window = parse_int(request.params.get("window"), 20)
    rows = parse_rows(request.params.get("rows"))
    result = {"pipeline": "msi.build_turnover_activity", "dry_run": request.dry_run, "trade_date": trade_date.isoformat(), "rows_read": 0, "rows_written": 0}
    if request.dry_run:
        payload = calculate_turnover_activity(rows or [], trade_date=trade_date, window=window, score=_score_param(request))
        return {**result, "rows_read": len(rows or []), "payload": payload, "notes": ["dry_run skipped ClickHouse and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    input_rows = rows if rows is not None else read_turnover_activity_rows(context.hooks.clickhouse.get_client(), trade_date, table=str(request.params.get("source_table") or "daily_raw"), limit_days=window + 10)
    payload = calculate_turnover_activity(input_rows, trade_date=trade_date, window=window, score=_score_param(request))
    if payload is None:
        return {**result, "rows_read": len(input_rows), "skipped": True, "skip_reason": "insufficient_turnover_input"}
    repo = MsiRepository(context.hooks.postgres.get_client())
    if truthy(request.params.get("create_table")):
        repo.create_table()
    result["rows_read"] = len(input_rows)
    result["rows_written"] = repo.save_turnover_activity(payload)
    result["payload"] = payload
    return result


def _score_param(request: JobRequest) -> float | None:
    value = request.params.get("score")
    return float(value) if value not in (None, "") else None
