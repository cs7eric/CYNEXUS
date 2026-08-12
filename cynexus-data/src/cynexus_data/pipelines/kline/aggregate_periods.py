from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.config.settings import load_yaml_config
from cynexus_data.storage.clickhouse.readers import aggregate_kline_period_to_clickhouse, count_aggregate_kline_bars, count_kline_rows


def run(context: PipelineContext, request: JobRequest) -> dict:
    periods = _normalize_periods(request.params.get("periods") or [5, 15, 30, 60])
    start_date = request.params.get("start_date") or request.params.get("trade_date")
    end_date = request.params.get("end_date") or request.params.get("trade_date")
    symbols = _normalize_symbols(request.params.get("symbols") or request.params.get("codes"))
    allow_full_history = _truthy(request.params.get("allow_full_history"))
    estimate = _truthy(request.params.get("estimate"))
    table = request.params.get("table") or _default_kline_table()

    result = {
        "pipeline": "kline.aggregate_periods",
        "dry_run": request.dry_run,
        "table": table,
        "periods": periods,
        "start_date": start_date,
        "end_date": end_date,
        "symbols": symbols,
        "allow_full_history": allow_full_history,
        "rows_read": 0,
        "rows_written": 0,
        "period_results": [],
    }
    if not request.dry_run and not allow_full_history and start_date is None and end_date is None and not symbols:
        raise ValueError(
            "aggregate kline requires trade_date/start_date/end_date or symbols; "
            "set allow_full_history=true only for an intentional maintenance backfill"
        )
    if request.dry_run and not estimate:
        result["notes"] = ["dry_run skipped ClickHouse access; pass estimate=true to count source and target rows"]
        return result

    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    client = context.hooks.clickhouse.get_client()
    source_rows = count_kline_rows(client, table=table, period=1, start_date=start_date, end_date=end_date, symbols=symbols)
    result["rows_read"] = source_rows

    total_written = 0
    for period in periods:
        expected_rows = count_aggregate_kline_bars(
            client,
            interval_minutes=period,
            source_table=table,
            start_date=start_date,
            end_date=end_date,
            symbols=symbols,
        )
        written = 0
        if not request.dry_run:
            written = aggregate_kline_period_to_clickhouse(
                client,
                interval_minutes=period,
                source_table=table,
                target_table=table,
                start_date=start_date,
                end_date=end_date,
                symbols=symbols,
                allow_full_history=allow_full_history,
            )
        total_written += written
        result["period_results"].append({"period": period, "expected_rows": expected_rows, "rows_written": written})
    result["rows_written"] = total_written
    return result


def _default_kline_table() -> str:
    config = load_yaml_config("database.yaml")
    return str(config.get("clickhouse", {}).get("tables", {}).get("kline_1m", "ch_dwd_kline_1m"))


def _normalize_periods(value) -> list[int]:
    if isinstance(value, str):
        raw_items = [item.strip() for item in value.split(",") if item.strip()]
    elif isinstance(value, (list, tuple, set)):
        raw_items = list(value)
    else:
        raw_items = [value]
    periods = sorted({int(item) for item in raw_items})
    if not periods:
        raise ValueError("at least one aggregate period is required")
    for period in periods:
        if period <= 1:
            raise ValueError("aggregate periods must be greater than 1")
    return periods


def _normalize_symbols(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
