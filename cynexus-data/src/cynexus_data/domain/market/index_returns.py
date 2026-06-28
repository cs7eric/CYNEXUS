from __future__ import annotations

from datetime import date
from typing import Any


INDEX_TARGETS: list[dict[str, str]] = [
    {"name": "上证指数", "code": "000001", "fullCode": "sh000001", "exchange": "sh"},
    {"name": "沪深300", "code": "000300", "fullCode": "sh000300", "exchange": "sh"},
    {"name": "中证1000", "code": "000852", "fullCode": "sh000852", "exchange": "sh"},
]


def calculate_index_return_item(
    target: dict[str, str],
    bars: list[dict[str, Any]],
    *,
    window_days: int,
) -> dict[str, Any]:
    days = max(1, min(int(window_days), 60))
    if not bars:
        return _empty_item(target)
    recent = bars[-days:] if len(bars) >= days else bars
    daily: list[dict[str, Any]] = []
    for index, row in enumerate(recent):
        close = _float(row.get("close"))
        if index == 0:
            daily_return = None
        else:
            prev_close = _float(recent[index - 1].get("close"))
            daily_return = round((close - prev_close) / prev_close * 100, 4) if prev_close > 0 else None
        daily.append({"date": _date_text(row.get("trade_date")), "close": close, "dailyReturnPct": daily_return})
    current = _float(recent[-1].get("close"))
    base_close = _float(recent[0].get("close"))
    return_pct = round((current - base_close) / base_close * 100, 4) if base_close > 0 else None
    return {
        "name": target.get("name") or "",
        "code": target.get("code") or _short_code(target.get("fullCode")),
        "fullCode": target.get("fullCode") or "",
        "current": current,
        "currentDate": _date_text(recent[-1].get("trade_date")),
        "baseClose": base_close,
        "baseDate": _date_text(recent[0].get("trade_date")),
        "returnPct": return_pct,
        "daily": daily,
        "available": True,
    }


def calculate_index_returns(
    rows_by_code: dict[str, list[dict[str, Any]]],
    *,
    window_days: int = 5,
    targets: list[dict[str, str]] | None = None,
) -> list[dict[str, Any]]:
    return [
        calculate_index_return_item(target, rows_by_code.get(str(target.get("fullCode") or "")) or [], window_days=window_days)
        for target in (targets or INDEX_TARGETS)
    ]


def normalize_index_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if not value:
        return ""
    if value.startswith(("sh", "sz", "bj")):
        return value
    if value.isdigit():
        return f"sh{value}"
    return value


def _empty_item(target: dict[str, str]) -> dict[str, Any]:
    return {
        "name": target.get("name") or "",
        "code": target.get("code") or _short_code(target.get("fullCode")),
        "fullCode": target.get("fullCode") or "",
        "current": None,
        "currentDate": None,
        "baseClose": None,
        "baseDate": None,
        "returnPct": None,
        "daily": [],
        "available": False,
    }


def _short_code(full_code: str | None) -> str:
    if not full_code:
        return ""
    value = str(full_code)
    return value[2:] if value[:2].lower() in {"sh", "sz", "bj"} else value


def _date_text(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, date):
        return value.isoformat()
    return str(value)[:10]


def _float(value: Any) -> float:
    return float(value) if value is not None else 0.0
