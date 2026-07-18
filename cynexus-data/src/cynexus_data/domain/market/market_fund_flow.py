from __future__ import annotations

from datetime import date
from typing import Any


FLOW_FIELDS = (
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
)


def normalize_market_fund_flow_row(row: dict[str, Any]) -> dict[str, Any]:
    trade_date = _parse_date(row.get("trade_date"))
    if trade_date is None:
        raise ValueError(f"market fund-flow row has no valid trade_date: {row}")

    return {
        "trade_date": trade_date,
        "main_net_inflow": _to_yi(row.get("main_net_inflow")),
        "super_large_net_inflow": _to_yi(row.get("super_large_net_inflow")),
        "large_net_inflow": _to_yi(row.get("large_net_inflow")),
        "medium_net_inflow": _to_yi(row.get("medium_net_inflow")),
        "small_net_inflow": _to_yi(row.get("small_net_inflow")),
        "main_net_inflow_ratio": _to_float(row.get("main_net_inflow_ratio")),
        "super_large_net_ratio": _to_float(row.get("super_large_net_inflow_ratio")),
        "large_net_ratio": _to_float(row.get("large_net_inflow_ratio")),
        "medium_net_ratio": _to_float(row.get("medium_net_inflow_ratio")),
        "small_net_ratio": _to_float(row.get("small_net_inflow_ratio")),
        "raw_payload": dict(row),
    }


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value).strip()
    if len(text) >= 10:
        text = text[:10]
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text)


def _to_yi(value: Any) -> float | None:
    parsed = _to_float(value)
    return round(parsed / 1e8, 4) if parsed is not None else None


def _to_float(value: Any) -> float | None:
    if value in (None, "", "-", "--"):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None
