from __future__ import annotations

import json
from datetime import date, timedelta
from typing import Any

from cynexus_data.domain.market.market_technical import (
    build_industry_strength_overview,
    build_sentiment_overview,
    build_similar_scenario_backtest,
    build_style_overview,
    build_support_resistance,
    dominant_style_from_rows,
    infer_regime_bucket,
    latest_window_metrics,
    moving_average,
)
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_daily_bars_by_code
from cynexus_data.storage.postgres.market_overview import MarketOverviewRepository


DEFAULT_STYLE_INDICES = {
    "sse50": "000016",
    "hs300": "000300",
    "csi500": "000905",
    "csi1000": "000852",
    "csi2000": "932000",
    "chinext": "399006",
    "star50": "000688",
}


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    params = request.params
    trade_date = _parse_date(params.get("trade_date") or date.today())
    benchmark_code = str(params.get("benchmark_code") or params.get("code") or "999999")
    source_table = str(params.get("source_table") or "daily_raw")
    lookback_days = int(params.get("lookback_days") or 380)
    create_table = _truthy(params.get("create_table"))

    benchmark_bars = _json_list(params.get("benchmark_rows") or params.get("rows"))
    index_bars_map = _json_mapping_of_lists(params.get("index_bars_map"))
    breadth_series = _json_list(params.get("breadth_series"))
    today_breadth = _json_dict(params.get("today_breadth"))
    industry_rows = _json_list(params.get("industry_rows"))
    rows_read = len(benchmark_bars) + sum(len(rows) for rows in index_bars_map.values()) + len(breadth_series) + len(industry_rows)
    notes: list[str] = []

    if request.dry_run:
        if not benchmark_bars:
            benchmark_bars = _sample_bars(220)
            notes.append("dry_run used synthetic benchmark rows")
        if not index_bars_map:
            index_bars_map = {"hs300": _sample_bars(220, step=0.22), "csi1000": _sample_bars(220, step=0.28)}
            notes.append("dry_run used synthetic style index rows")
        if not breadth_series:
            breadth_series = _sample_breadth_series(len(benchmark_bars))
        if not today_breadth and breadth_series:
            today_breadth = breadth_series[-1]
        rows_read = len(benchmark_bars) + sum(len(rows) for rows in index_bars_map.values()) + len(breadth_series) + len(industry_rows)
    else:
        if context.hooks is None:
            raise RuntimeError("PipelineContext.hooks is required")
        pg_client = context.hooks.postgres.get_client()
        repo = MarketOverviewRepository(pg_client)
        if create_table:
            repo.create_table()
        if not benchmark_bars:
            ch_client = context.hooks.clickhouse.get_client()
            benchmark_bars = read_daily_bars_by_code(ch_client, benchmark_code, trade_date, table=source_table, lookback_days=lookback_days)
            rows_read += len(benchmark_bars)
            style_indices = _json_dict(params.get("style_indices")) or DEFAULT_STYLE_INDICES
            for name, code in style_indices.items():
                rows = read_daily_bars_by_code(ch_client, str(code), trade_date, table=source_table, lookback_days=lookback_days)
                if rows:
                    index_bars_map[str(name)] = rows
                    rows_read += len(rows)
        if not today_breadth:
            today_breadth = repo.get(trade_date) or {}
            rows_read += 1 if today_breadth else 0
        if not breadth_series:
            breadth_series = repo.list_snapshots(end_date=trade_date, limit=min(lookback_days, 380))
            rows_read += len(breadth_series)

    payload = _build_detail_payload(
        benchmark_bars=benchmark_bars,
        benchmark_code=benchmark_code,
        trade_date=trade_date,
        source_table=source_table,
        index_bars_map=index_bars_map,
        breadth_series=breadth_series,
        today_breadth=today_breadth,
        industry_rows=industry_rows,
    )
    result = {
        "pipeline": "market_overview.build_market_overview_detail",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "benchmark_code": benchmark_code,
        "source_table": source_table,
        "rows_read": rows_read,
        "rows_written": 0,
        "payload": payload,
        "notes": notes,
    }
    if request.dry_run:
        return result

    row = MarketOverviewRepository(context.hooks.postgres.get_client()).upsert_detail(
        trade_date=trade_date,
        benchmark_code=benchmark_code,
        source_table=source_table,
        payload=payload,
        source=str(params.get("source") or "python"),
    )
    result["rows_written"] = 1
    result["snapshot"] = _jsonable(row)
    return result


def _build_detail_payload(
    *,
    benchmark_bars: list[dict[str, Any]],
    benchmark_code: str,
    trade_date: date,
    source_table: str,
    index_bars_map: dict[str, list[dict[str, Any]]],
    breadth_series: list[dict[str, Any]],
    today_breadth: dict[str, Any],
    industry_rows: list[dict[str, Any]],
) -> dict[str, Any]:
    metrics = latest_window_metrics(benchmark_bars)
    closes = [float(item["close"]) for item in benchmark_bars if item.get("close") is not None]
    ma20 = _last_ma(closes, 20)
    ma60 = _last_ma(closes, 60)
    ma120 = _last_ma(closes, 120)
    ma250 = _last_ma(closes, 250)
    supports, resistances = build_support_resistance(benchmark_bars, metrics, ma20=ma20, ma60=ma60, ma120=ma120, ma250=ma250)
    wm20 = next((item for item in metrics if item["window"] == 20), {})
    wm60 = next((item for item in metrics if item["window"] == 60), {})
    regime = infer_regime_bucket(
        wm20.get("rangePosition"),
        wm60.get("rangePosition"),
        bool(wm20.get("closeAboveMa20")),
        bool(wm20.get("closeAboveMa60")),
        wm20.get("returnN"),
        wm60.get("returnN"),
    )
    style_rows = build_style_overview(index_bars_map, benchmark_bars)
    dominant_style = dominant_style_from_rows(style_rows)
    sentiment = build_sentiment_overview(today_breadth, breadth_series)
    industry_strength = build_industry_strength_overview(industry_rows, benchmark_bars, index_bars_map) if industry_rows else []
    similar = build_similar_scenario_backtest(
        benchmark_bars,
        regime=regime,
        window_metrics=metrics,
        sentiment=sentiment,
        dominant_style=dominant_style,
        index_bars_map=index_bars_map,
        breadth_series=breadth_series,
    )
    return {
        "tradeDate": trade_date.isoformat(),
        "benchmarkCode": benchmark_code,
        "sourceTable": source_table,
        "barCount": len(benchmark_bars),
        "latestBar": benchmark_bars[-1] if benchmark_bars else None,
        "windowMetrics": metrics,
        "supportLevels": supports,
        "resistanceLevels": resistances,
        "regimeBucket": regime,
        "movingAverages": {"ma20": ma20, "ma60": ma60, "ma120": ma120, "ma250": ma250},
        "styleRows": style_rows,
        "dominantStyle": dominant_style,
        "industryStrength": industry_strength[:30],
        "sentiment": sentiment,
        "similarScenario": similar,
    }


def _last_ma(closes: list[float], period: int) -> float | None:
    values = moving_average(closes, period)
    value = values[-1] if values else None
    return round(float(value), 4) if value is not None else None


def _json_dict(value: Any) -> dict[str, Any]:
    if value in (None, ""):
        return {}
    if isinstance(value, dict):
        return dict(value)
    parsed = json.loads(str(value))
    if not isinstance(parsed, dict):
        raise ValueError("expected a JSON object")
    return parsed


def _json_list(value: Any) -> list[dict[str, Any]]:
    if value in (None, ""):
        return []
    if isinstance(value, list):
        return [dict(item) for item in value if isinstance(item, dict)]
    parsed = json.loads(str(value))
    if not isinstance(parsed, list):
        raise ValueError("expected a JSON array")
    return [dict(item) for item in parsed if isinstance(item, dict)]


def _json_mapping_of_lists(value: Any) -> dict[str, list[dict[str, Any]]]:
    if value in (None, ""):
        return {}
    if isinstance(value, dict):
        raw = value
    else:
        raw = json.loads(str(value))
    if not isinstance(raw, dict):
        raise ValueError("expected a JSON object of arrays")
    return {str(key): [dict(item) for item in rows if isinstance(item, dict)] for key, rows in raw.items() if isinstance(rows, list)}


def _parse_date(value: Any) -> date:
    if isinstance(value, date):
        return value
    text = str(value)
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text[:10])


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _jsonable(row: dict[str, Any]) -> dict[str, Any]:
    return {key: value.isoformat() if hasattr(value, "isoformat") else value for key, value in row.items()}


def _sample_bars(count: int, *, step: float = 0.2) -> list[dict[str, Any]]:
    start = date(2025, 1, 1)
    price = 100.0
    rows: list[dict[str, Any]] = []
    for index in range(count):
        price += step + ((index % 9) - 4) * 0.03
        rows.append(
            {
                "trade_date": (start + timedelta(days=index)).isoformat(),
                "open": round(price - 0.3, 4),
                "high": round(price + 0.8, 4),
                "low": round(price - 0.8, 4),
                "close": round(price, 4),
                "volume": 1000 + index * 10,
                "amount": round((1000 + index * 10) * price, 4),
            }
        )
    return rows


def _sample_breadth_series(count: int) -> list[dict[str, Any]]:
    return [
        {
            "trade_date": (date(2025, 1, 1) + timedelta(days=index)).isoformat(),
            "rising_count": 2400 + (index % 30),
            "falling_count": 1900 - (index % 20),
            "limit_up_count": 40 + (index % 10),
            "limit_down_count": 8,
            "stock_count": 5200,
        }
        for index in range(count)
    ]
