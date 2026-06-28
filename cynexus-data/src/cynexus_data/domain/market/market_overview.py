from __future__ import annotations

from datetime import date
from typing import Any


MARKET_FIELDS = {
    "total_amount",
    "total_volume",
    "rising_count",
    "falling_count",
    "flat_count",
    "limit_up_count",
    "limit_down_count",
    "stock_count",
}
FUND_FLOW_FIELDS = {
    "main_net_inflow",
    "super_large_net_inflow",
    "large_net_inflow",
    "medium_net_inflow",
    "small_net_inflow",
    "main_net_inflow_ratio",
    "super_large_net_ratio",
    "large_net_ratio",
    "medium_net_ratio",
    "small_net_ratio",
}
NUMERIC_FIELDS = MARKET_FIELDS | FUND_FLOW_FIELDS
COUNT_FIELDS = {"rising_count", "falling_count", "flat_count", "limit_up_count", "limit_down_count", "stock_count"}
SOURCE_OVERWRITE_FIELDS = {
    "eltdx": MARKET_FIELDS,
    "akshare": FUND_FLOW_FIELDS,
    "eastmoney": MARKET_FIELDS,
    "tencent": MARKET_FIELDS,
    "manual": FUND_FLOW_FIELDS,
}


ALIASES = {
    "tradingDate": "trade_date",
    "tradeDate": "trade_date",
    "totalAmount": "total_amount",
    "totalVolume": "total_volume",
    "risingCount": "rising_count",
    "fallingCount": "falling_count",
    "flatCount": "flat_count",
    "limitUpCount": "limit_up_count",
    "limitDownCount": "limit_down_count",
    "stockCount": "stock_count",
    "mainNetInflow": "main_net_inflow",
    "superLargeNetInflow": "super_large_net_inflow",
    "largeNetInflow": "large_net_inflow",
    "mediumNetInflow": "medium_net_inflow",
    "smallNetInflow": "small_net_inflow",
    "mainNetInflowRatio": "main_net_inflow_ratio",
    "superLargeNetInflowRatio": "super_large_net_ratio",
    "largeNetInflowRatio": "large_net_ratio",
    "mediumNetInflowRatio": "medium_net_ratio",
    "smallNetInflowRatio": "small_net_ratio",
}


def normalize_market_overview_fields(payload: dict[str, Any], *, trade_date: date | None = None) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in payload.items():
        normalized_key = ALIASES.get(str(key), str(key))
        if normalized_key == "trade_date":
            out["trade_date"] = _parse_date(value)
        elif normalized_key in COUNT_FIELDS:
            out[normalized_key] = _to_int(value)
        elif normalized_key in NUMERIC_FIELDS:
            out[normalized_key] = _to_float(value)
    if trade_date is not None:
        out["trade_date"] = trade_date
    if "trade_date" not in out or out["trade_date"] is None:
        raise ValueError("trade_date/tradingDate is required")
    return out


def should_overwrite_field(*, source: str, field: str, current_value: Any) -> bool:
    if current_value is None:
        return True
    return field in SOURCE_OVERWRITE_FIELDS.get(source, set())


def merge_source(existing: str | None, source: str) -> str:
    if not existing:
        return source
    parts = {part for part in existing.split("+") if part}
    parts.add(source)
    return "+".join(sorted(parts))


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value)
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text)


def _to_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _to_int(value: Any) -> int | None:
    parsed = _to_float(value)
    return int(parsed) if parsed is not None else None
