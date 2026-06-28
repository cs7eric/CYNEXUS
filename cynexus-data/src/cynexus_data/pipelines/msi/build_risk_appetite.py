from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.msi_factors import calculate_risk_appetite
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_close_series
from cynexus_data.storage.postgres.msi import MsiRepository
from cynexus_data.storage.postgres.percentile import PercentileRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    window = parse_int(request.params.get("window"), 20)
    result = {"pipeline": "msi.build_risk_appetite", "dry_run": request.dry_run, "trade_date": trade_date.isoformat(), "rows_read": 0, "rows_written": 0}
    hs300_rows = parse_rows(request.params.get("hs300_rows"))
    t1_rows = parse_rows(request.params.get("treasury_511010_rows"))
    t2_rows = parse_rows(request.params.get("treasury_511090_rows"))
    if request.dry_run:
        payload = calculate_risk_appetite(trade_date=trade_date, hs300_rows=hs300_rows or [], treasury_511010_rows=t1_rows or [], treasury_511090_rows=t2_rows or [], window=window, score=_score_param(request))
        return {**result, "rows_read": len(hs300_rows or []) + len(t1_rows or []) + len(t2_rows or []), "payload": payload, "notes": ["dry_run skipped ClickHouse and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    ch = context.hooks.clickhouse.get_client()
    table = str(request.params.get("source_table") or "daily_qfq")
    hs300_rows = hs300_rows if hs300_rows is not None else read_close_series(ch, str(request.params.get("hs300_code") or "000300"), trade_date, table=table, limit=window + 1)
    t1_rows = t1_rows if t1_rows is not None else read_close_series(ch, "511010", trade_date, table=table, limit=window + 1)
    t2_rows = t2_rows if t2_rows is not None else read_close_series(ch, "511090", trade_date, table=table, limit=window + 1)
    payload = calculate_risk_appetite(trade_date=trade_date, hs300_rows=hs300_rows, treasury_511010_rows=t1_rows, treasury_511090_rows=t2_rows, window=window, score=_score_param(request))
    if payload is None:
        return {**result, "rows_read": len(hs300_rows) + len(t1_rows) + len(t2_rows), "skipped": True, "skip_reason": "insufficient_risk_appetite_input"}
    pg_client = context.hooks.postgres.get_client()
    if _score_param(request) is None:
        payload["score"] = PercentileRepository(pg_client).percentile_score("risk_appetite_daily", "spread_weighted", trade_date, payload.get("rawValue"))
    repo = MsiRepository(pg_client)
    if truthy(request.params.get("create_table")):
        repo.create_table()
    result["rows_read"] = len(hs300_rows) + len(t1_rows) + len(t2_rows)
    result["rows_written"] = repo.save_risk_appetite(payload)
    result["payload"] = payload
    return result


def _score_param(request: JobRequest) -> float | None:
    value = request.params.get("score")
    return float(value) if value not in (None, "") else None
