from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.msi_factors import calculate_volatility_sentiment
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_close_series
from cynexus_data.storage.postgres.msi import MsiRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    window = parse_int(request.params.get("window"), 20)
    lookback = parse_int(request.params.get("lookback"), 252)
    code = str(request.params.get("underlying_code") or "sh000300")
    name = str(request.params.get("underlying_name") or "沪深300")
    rows = parse_rows(request.params.get("rows"))
    result = {"pipeline": "msi.build_volatility_sentiment", "dry_run": request.dry_run, "trade_date": trade_date.isoformat(), "rows_read": 0, "rows_written": 0}
    if request.dry_run:
        payload = calculate_volatility_sentiment(rows or [], trade_date=trade_date, underlying_code=code, underlying_name=name, window=window, lookback=lookback)
        return {**result, "rows_read": len(rows or []), "payload": payload, "notes": ["dry_run skipped ClickHouse and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    input_rows = rows if rows is not None else read_close_series(context.hooks.clickhouse.get_client(), code, trade_date, table=str(request.params.get("source_table") or "index_daily_raw"), limit=lookback + window + 10)
    payload = calculate_volatility_sentiment(input_rows, trade_date=trade_date, underlying_code=code, underlying_name=name, window=window, lookback=lookback)
    if payload is None:
        return {**result, "rows_read": len(input_rows), "skipped": True, "skip_reason": "insufficient_volatility_input"}
    repo = MsiRepository(context.hooks.postgres.get_client())
    if truthy(request.params.get("create_table")):
        repo.create_table()
    result["rows_read"] = len(input_rows)
    result["rows_written"] = repo.save_volatility_sentiment(payload)
    result["payload"] = payload
    return result
