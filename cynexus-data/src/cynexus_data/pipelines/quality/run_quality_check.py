from __future__ import annotations

from cynexus_data.config.settings import load_yaml_config
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import (
    count_duplicate_daily_keys,
    count_invalid_ohlcv_rows,
    count_kline_rows,
    count_scaled_duplicate_daily_keys,
    sample_daily_rows_for_source_validation,
)
from cynexus_data.sources.provider_fallback import fetch_daily_kline_with_fallback


def run(context: PipelineContext, request: JobRequest) -> dict:
    checks = _normalize_checks(request.params.get("checks") or ["kline_ohlcv"])
    estimate = _truthy(request.params.get("estimate"))
    period = int(request.params.get("period") or 1)
    start_date = request.params.get("start_date") or request.params.get("trade_date")
    end_date = request.params.get("end_date") or request.params.get("trade_date")
    symbols = _normalize_symbols(request.params.get("symbols") or request.params.get("codes"))
    table = request.params.get("table") or _default_kline_table()
    daily_table = str(request.params.get("daily_table") or "daily_raw")
    ratio_threshold = float(request.params.get("ratio_threshold") or 100.0)
    sample_every = int(request.params.get("sample_every") or 100)
    sample_limit = _optional_int(request.params.get("sample_limit"))
    sample_markets = _normalize_optional_list(request.params.get("sample_markets"))
    sample_scope = str(request.params.get("sample_scope") or "all").lower()
    source_limit = int(request.params.get("source_limit") or 30)
    source_providers = _normalize_providers(request.params.get("source_providers") or request.params.get("providers") or ["tencent", "eastmoney"])
    price_tolerance_pct = float(request.params.get("price_tolerance_pct") or 0.05)
    price_tolerance_abs = float(request.params.get("price_tolerance_abs") or 0.01)
    result = {
        "pipeline": "quality.run_quality_check",
        "dry_run": request.dry_run,
        "checks": checks,
        "table": table,
        "daily_table": daily_table,
        "period": period,
        "start_date": start_date,
        "end_date": end_date,
        "symbols": symbols,
        "sample_every": sample_every,
        "sample_limit": sample_limit,
        "sample_markets": sample_markets,
        "sample_scope": sample_scope,
        "source_providers": source_providers,
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
    if "kline_ohlcv" in checks:
        rows = count_kline_rows(client, table=table, period=period, start_date=start_date, end_date=end_date, symbols=symbols)
        result["rows_read"] = rows
        invalid_rows = count_invalid_ohlcv_rows(client, table=table, period=period, start_date=start_date, end_date=end_date, symbols=symbols)
        result["check_results"].append({"check": "kline_ohlcv", "ok": invalid_rows == 0, "invalid_rows": invalid_rows})
    if "daily_duplicate_keys" in checks:
        duplicate_keys = count_duplicate_daily_keys(client, table=daily_table, start_date=start_date, end_date=end_date)
        result["check_results"].append({"check": "daily_duplicate_keys", "ok": duplicate_keys == 0, "duplicate_keys": duplicate_keys, "table": daily_table})
    if "daily_scale_anomaly" in checks:
        anomaly_keys = count_scaled_duplicate_daily_keys(
            client,
            table=daily_table,
            start_date=start_date,
            end_date=end_date,
            ratio_threshold=ratio_threshold,
        )
        result["check_results"].append(
            {
                "check": "daily_scale_anomaly",
                "ok": anomaly_keys == 0,
                "anomaly_keys": anomaly_keys,
                "ratio_threshold": ratio_threshold,
                "table": daily_table,
            }
        )
    if "daily_source_sample" in checks:
        if end_date is None:
            raise ValueError("daily_source_sample requires trade_date or end_date")
        sample_result = _run_daily_source_sample_check(
            context,
            client,
            daily_table=daily_table,
            trade_date=end_date,
            sample_every=sample_every,
            sample_limit=sample_limit,
            sample_markets=sample_markets,
            sample_scope=sample_scope,
            source_limit=source_limit,
            source_providers=source_providers,
            price_tolerance_pct=price_tolerance_pct,
            price_tolerance_abs=price_tolerance_abs,
        )
        result["rows_read"] += sample_result["sampled_rows"]
        result["check_results"].append(sample_result)
    return result


def _run_daily_source_sample_check(
    context: PipelineContext,
    client,
    *,
    daily_table: str,
    trade_date,
    sample_every: int,
    sample_limit: int | None,
    sample_markets: list[str] | None,
    sample_scope: str,
    source_limit: int,
    source_providers: list[str],
    price_tolerance_pct: float,
    price_tolerance_abs: float,
) -> dict:
    sampled_rows = sample_daily_rows_for_source_validation(
        client,
        table=daily_table,
        trade_date=trade_date,
        sample_every=sample_every,
        limit=sample_limit,
        markets=sample_markets,
        sample_scope=sample_scope,
    )
    mismatches: list[dict] = []
    missing_source_rows: list[dict] = []
    fetch_errors: list[dict] = []
    checked = 0
    for row in sampled_rows:
        symbol = str(row.get("tdx_code") or row.get("symbol") or row.get("code"))
        fetched, source_row = _fetch_source_row_for_trade_date(
            context,
            symbol=symbol,
            trade_date=row["trade_date"],
            providers=source_providers,
            source_limit=source_limit,
        )
        if source_row is None:
            if fetched.source == "none":
                fetch_errors.append({"symbol": symbol, "attempts": [attempt.__dict__ for attempt in fetched.attempts]})
            else:
                missing_source_rows.append({"symbol": symbol, "trade_date": row["trade_date"], **fetched.meta()})
            continue
        checked += 1
        diffs = _price_diffs(row, source_row, tolerance_pct=price_tolerance_pct, tolerance_abs=price_tolerance_abs)
        if diffs:
            mismatches.append(
                {
                    "symbol": symbol,
                    "trade_date": row["trade_date"],
                    "source": fetched.source,
                    "diffs": diffs,
                    "clickhouse": {key: row.get(key) for key in ["open", "high", "low", "close"]},
                    "source_row": {key: _float_or_none(source_row.get(key)) for key in ["open", "high", "low", "close"]},
                }
            )
    ok = not mismatches and not missing_source_rows and not fetch_errors
    return {
        "check": "daily_source_sample",
        "ok": ok,
        "table": daily_table,
        "trade_date": str(trade_date)[:10],
        "sample_every": sample_every,
        "sample_markets": sample_markets,
        "sample_scope": sample_scope,
        "sampled_rows": len(sampled_rows),
        "checked_rows": checked,
        "mismatch_count": len(mismatches),
        "missing_source_rows": len(missing_source_rows),
        "fetch_error_count": len(fetch_errors),
        "price_tolerance_pct": price_tolerance_pct,
        "price_tolerance_abs": price_tolerance_abs,
        "providers": source_providers,
        "mismatches": mismatches[:20],
        "missing_samples": missing_source_rows[:20],
        "fetch_errors": fetch_errors[:20],
    }


def _fetch_source_row_for_trade_date(
    context: PipelineContext,
    *,
    symbol: str,
    trade_date,
    providers: list[str],
    source_limit: int,
):
    last_fetch = None
    for provider in providers:
        fetched = fetch_daily_kline_with_fallback(context.hooks, symbol=symbol, providers=[provider], adjust="none", limit=source_limit)
        source_row = _find_source_row(fetched.items, trade_date)
        if source_row is not None:
            return fetched, source_row
        last_fetch = fetched
    if last_fetch is None:
        fetched = fetch_daily_kline_with_fallback(context.hooks, symbol=symbol, providers=[], adjust="none", limit=source_limit)
        return fetched, None
    return last_fetch, None


def _find_source_row(rows: list[dict], trade_date) -> dict | None:
    target = str(trade_date)[:10]
    for row in rows:
        row_date = row.get("trade_date")
        if row_date is None and row.get("trade_time") is not None:
            row_date = row["trade_time"].date() if hasattr(row["trade_time"], "date") else str(row["trade_time"])[:10]
        if str(row_date)[:10] == target:
            return row
    return None


def _price_diffs(ch_row: dict, source_row: dict, *, tolerance_pct: float, tolerance_abs: float) -> list[dict]:
    diffs = []
    for field in ["open", "high", "low", "close"]:
        ch_value = _float_or_none(ch_row.get(field))
        source_value = _float_or_none(source_row.get(field))
        if ch_value is None or source_value is None:
            diffs.append({"field": field, "clickhouse": ch_value, "source": source_value, "reason": "missing"})
            continue
        abs_diff = abs(ch_value - source_value)
        pct_diff = abs_diff / abs(source_value) * 100 if source_value else abs_diff * 100
        if abs_diff > tolerance_abs and pct_diff > tolerance_pct:
            diffs.append({"field": field, "clickhouse": ch_value, "source": source_value, "abs_diff": abs_diff, "pct_diff": pct_diff})
    return diffs


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


def _normalize_providers(value) -> list[str]:
    if isinstance(value, str):
        return [item.strip().lower() for item in value.split(",") if item.strip()]
    return [str(item).strip().lower() for item in value if str(item).strip()]


def _normalize_optional_list(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip().upper() for item in value.split(",") if item.strip()]
    return [str(item).strip().upper() for item in value if str(item).strip()]


def _optional_int(value) -> int | None:
    if value in (None, ""):
        return None
    return int(value)


def _float_or_none(value) -> float | None:
    if value is None:
        return None
    return float(value)


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
