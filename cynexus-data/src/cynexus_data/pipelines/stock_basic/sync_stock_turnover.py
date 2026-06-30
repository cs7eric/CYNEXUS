from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.core.calendar import is_trading_day
from cynexus_data.domain.stock.turnover import build_turnover_series
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.stock_profile import create_eltdx_stock_profile_client, fetch_stock_share_profile
from cynexus_data.storage.clickhouse.readers import read_daily_kline_bars
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository
from cynexus_data.storage.postgres.stock_turnover import StockTurnoverRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    period = str(request.params.get("period") or "1d")
    adjust = str(request.params.get("adjust") or "qfq")
    target_type = str(request.params.get("target_type") or "stock")
    source = str(request.params.get("source") or "clickhouse+eltdx")
    limit = parse_int(request.params.get("limit") or request.params.get("bar_limit"), 260)
    create_table = truthy(request.params.get("create_table"))
    trading_day_only = truthy(request.params.get("trading_day_only"))
    target_date = _parse_date(
        request.params.get("target_date")
        or request.params.get("trade_date")
        or request.params.get("snapshot_date")
        or request.params.get("date")
    )
    codes = _parse_csv(request.params.get("codes") or request.params.get("symbols"))
    bars_by_code = _parse_json_mapping(request.params.get("bars_by_code"))
    shares_by_code = _parse_json_mapping(request.params.get("shares_by_code"))
    result = {
        "pipeline": "stock_basic.sync_stock_turnover",
        "dry_run": request.dry_run,
        "period": period,
        "adjust": adjust,
        "target_date": target_date.isoformat() if target_date else None,
        "trading_day_only": trading_day_only,
        "rows_read": 0,
        "rows_written": 0,
    }
    if trading_day_only and target_date is not None and not is_trading_day(target_date):
        result["skipped"] = True
        result["skip_reason"] = "non_trading_day"
        return result
    if request.dry_run:
        planned_codes = codes or list(bars_by_code) or ["000001"]
        series = [
            _build_one_series(
                code=code,
                target_type=target_type,
                period=period,
                adjust=adjust,
                bars=bars_by_code.get(code) or _sample_bars(),
                shares=shares_by_code.get(code) or {"circulating_shares": 1000000000, "total_shares": 1200000000},
                source=source,
            )
            for code in planned_codes[: parse_int(request.params.get("limit_codes"), len(planned_codes))]
        ]
        return {**result, "rows_read": sum(len(item["entries"]) for item in series), "planned_rows": len(series), "sample": series[:2], "notes": ["dry_run skipped ClickHouse, eltdx, and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    if not codes:
        codes = StockBasicRepository(pg).list_codes(active_only=True)
    limit_codes = parse_int(request.params.get("limit_codes"), len(codes))
    codes = codes[:limit_codes]
    ch = context.hooks.clickhouse.get_client()
    repo = StockTurnoverRepository(pg)
    if create_table:
        repo.create_table()
    profile_client = None
    rows_read = 0
    rows_written = 0
    try:
        for code in codes:
            bars = bars_by_code.get(code) or read_daily_kline_bars(ch, code, adjust=adjust, limit=limit)
            shares = shares_by_code.get(code)
            if shares is None:
                if profile_client is None:
                    pool_size = parse_int(request.params.get("pool_size"), 4)
                    timeout = float(request.params.get("timeout") or request.params.get("timeout_seconds") or 8.0)
                    profile_client = create_eltdx_stock_profile_client(pool_size=pool_size, timeout=timeout)
                shares = fetch_stock_share_profile(profile_client, code)
            series = _build_one_series(code=code, target_type=target_type, period=period, adjust=adjust, bars=bars, shares=shares, source=source)
            rows_read += len(bars)
            rows_written += int(repo.upsert_series(series, source=source).get("entry_count") or len(series["entries"]))
    finally:
        close = getattr(profile_client, "close", None)
        if callable(close):
            close()
    return {**result, "codes_processed": len(codes), "rows_read": rows_read, "rows_written": rows_written}


def _build_one_series(
    *,
    code: str,
    target_type: str,
    period: str,
    adjust: str,
    bars: list[dict[str, Any]],
    shares: dict[str, Any],
    source: str,
) -> dict[str, Any]:
    return build_turnover_series(
        symbol=_normalize_code(code),
        target_type=target_type,
        period=period,
        adjust=adjust,
        bars=bars,
        circulating_shares=_float_or_none(shares.get("circulating_shares")),
        total_shares=_float_or_none(shares.get("total_shares")),
        source=source,
    )


def _parse_csv(value: Any) -> list[str]:
    if value in (None, ""):
        return []
    if isinstance(value, list):
        raw = value
    else:
        raw = str(value).split(",")
    return [_normalize_code(item) for item in raw if str(item).strip()]


def _parse_json_mapping(value: Any) -> dict[str, Any]:
    if value in (None, ""):
        return {}
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, dict):
        raise ValueError("mapping param must be a JSON object")
    return {_normalize_code(key): value for key, value in raw.items()}


def _normalize_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    for prefix in ("sh", "sz", "bj"):
        if text.startswith(prefix):
            text = text[len(prefix) :]
            break
    return text.zfill(6)


def _float_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _sample_bars() -> list[dict[str, Any]]:
    return [
        {"trade_date": "2026-06-25", "timestamp": 1790280600000, "volume": 10000, "amount": 100000000},
        {"trade_date": "2026-06-26", "timestamp": 1790367000000, "volume": 12000, "amount": 120000000},
    ]
