from __future__ import annotations

from cynexus_data.config.settings import load_yaml_config
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import count_invalid_ohlcv_rows, count_kline_rows


def run(context: PipelineContext, request: JobRequest) -> dict:
    checks = _normalize_checks(request.params.get("checks") or ["kline_ohlcv"])
    estimate = _truthy(request.params.get("estimate"))
    period = int(request.params.get("period") or 1)
    start_date = request.params.get("start_date") or request.params.get("trade_date")
    end_date = request.params.get("end_date") or request.params.get("trade_date")
    symbols = _normalize_symbols(request.params.get("symbols") or request.params.get("codes"))
    table = request.params.get("table") or _default_kline_table()
    result = {
        "pipeline": "quality.run_quality_check",
        "dry_run": request.dry_run,
        "checks": checks,
        "table": table,
        "period": period,
        "start_date": start_date,
        "end_date": end_date,
        "symbols": symbols,
        "rows_read": 0,
        "rows_written": 0,
        "check_results": [],
    }
    if request.dry_run and not estimate:
        result["notes"] = ["dry_run skipped ClickHouse reads; pass estimate=true to run read-only quality checks"]
        return result

    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    client = context.hooks.clickhouse.get_client()
    rows = count_kline_rows(client, table=table, period=period, start_date=start_date, end_date=end_date, symbols=symbols)
    result["rows_read"] = rows
    if "kline_ohlcv" in checks:
        invalid_rows = count_invalid_ohlcv_rows(client, table=table, period=period, start_date=start_date, end_date=end_date, symbols=symbols)
        result["check_results"].append({"check": "kline_ohlcv", "ok": invalid_rows == 0, "invalid_rows": invalid_rows})
    return result


def _default_kline_table() -> str:
    config = load_yaml_config("database.yaml")
    return str(config.get("clickhouse", {}).get("tables", {}).get("kline_1m", "ch_dwd_kline_1m"))


def _normalize_checks(value) -> list[str]:
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


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
