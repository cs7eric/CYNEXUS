from __future__ import annotations

from datetime import date
from typing import Any


def build_market_pulse_rows(rows: list[dict[str, Any]], *, trade_date: date) -> list[dict[str, Any]]:
    normalized = [_normalize_row(row, trade_date=trade_date) for row in rows]
    normalized = [row for row in normalized if row is not None]
    normalized = _dedupe_by_sector_name(normalized)
    normalized.sort(key=lambda row: (row.get("change_pct") is None, -(row.get("change_pct") or 0), row.get("sector_name") or ""))
    for rank, row in enumerate(normalized, start=1):
        row["rank_by_change"] = rank
    return normalized


def build_strong_and_flow_payload(rows: list[dict[str, Any]], *, trade_date: date, top_n: int = 10, flow_top_n: int = 20) -> dict[str, Any]:
    ordered = build_market_pulse_rows(rows, trade_date=trade_date)
    top = ordered[:top_n]
    bottom = list(reversed(ordered[-min(top_n, len(ordered)) :]))
    inflow = sorted([row for row in ordered if (row.get("main_net") or 0) > 0], key=lambda row: -(row.get("main_net") or 0))
    outflow = sorted([row for row in ordered if (row.get("main_net") or 0) <= 0], key=lambda row: row.get("main_net") or 0)
    return {
        "tradeDate": trade_date.isoformat(),
        "strong": {"top": top, "bottom": bottom, "count": len(ordered), "topN": top_n},
        "flow": {
            "inflow": inflow[:flow_top_n],
            "outflow": outflow[:flow_top_n],
            "inflowCount": len(inflow),
            "outflowCount": len(outflow),
            "totalIndustries": len(ordered),
            "unit": "亿",
        },
    }


def _normalize_row(row: dict[str, Any], *, trade_date: date) -> dict[str, Any] | None:
    sector_name = _strip(row.get("sector_name") or row.get("name") or row.get("行业"))
    if not sector_name:
        return None
    return {
        "trade_date": trade_date,
        "sector_name": sector_name,
        "sector_index": _strip(row.get("sector_index") or row.get("sector_code") or row.get("index") or row.get("code")),
        "change_pct": _to_float(row.get("change_pct") or row.get("changePct") or row.get("行业指数涨跌幅")),
        "inflow": _to_float(row.get("inflow") or row.get("流入资金(亿)")),
        "outflow": _to_float(row.get("outflow") or row.get("流出资金(亿)")),
        "main_net": _to_float(row.get("main_net") or row.get("net") or row.get("mainNet") or row.get("净额(亿)")),
        "stock_count": _to_int(row.get("stock_count") or row.get("company_count") or row.get("stockCount") or row.get("公司家数")),
        "leading_stock": _strip(row.get("leading_stock") or row.get("leader_stock") or row.get("leadingStock") or row.get("领涨股")),
        "leading_change_pct": _to_float(row.get("leading_change_pct") or row.get("leader_change") or row.get("leadingChangePct") or row.get("领涨股涨跌幅")),
        "leading_price": _to_float(row.get("leading_price") or row.get("leader_price") or row.get("leadingPrice") or row.get("当前价(元)")),
        "extra": dict(row),
    }


def _dedupe_by_sector_name(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    latest_by_name: dict[str, dict[str, Any]] = {}
    for row in rows:
        sector_name = str(row.get("sector_name") or "").strip()
        if not sector_name:
            continue
        existing = latest_by_name.get(sector_name)
        if existing is None or _row_completeness(row) >= _row_completeness(existing):
            latest_by_name[sector_name] = row
    return list(latest_by_name.values())


def _row_completeness(row: dict[str, Any]) -> int:
    keys = (
        "sector_index",
        "change_pct",
        "inflow",
        "outflow",
        "main_net",
        "stock_count",
        "leading_stock",
        "leading_change_pct",
        "leading_price",
    )
    return sum(1 for key in keys if row.get(key) not in (None, ""))


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


def _strip(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
