from __future__ import annotations

from datetime import date
from typing import Any


STYLE_CATEGORY_RAW = 4

STYLE_SECTOR_NAMES: tuple[str, ...] = (
    "近期新低",
    "微小盘股",
    "微盘股",
    "近期弱势",
    "昨日较弱",
    "最近情绪指数",
    "百元股",
    "近期新高",
    "大盘股",
    "低价股",
    "最近异动",
    "昨日弱势",
    "自由现金流",
    "行业龙头",
    "历史新低",
    "近期强势",
    "昨高换手",
    "昨日涨停",
    "机构吸筹",
    "最近多板",
    "昨日跌停",
    "昨日首板",
    "昨曾跌停",
    "历史新高",
    "昨日较强",
    "昨曾涨停",
    "昨日强势",
    "昨日连板",
    "昨日断板",
)


def build_style_sector_snapshots(
    sectors: list[dict[str, Any]],
    quotes_by_code: dict[str, dict[str, Any]],
    *,
    trade_date: date,
    allowed_names: tuple[str, ...] = STYLE_SECTOR_NAMES,
) -> list[dict[str, Any]]:
    allowed = set(allowed_names)
    snapshots: list[dict[str, Any]] = []
    for sector in sectors:
        name = str(sector.get("name") or sector.get("sector_name") or "").strip()
        if not name or name not in allowed:
            continue
        codes = [str(code).zfill(6) for code in (sector.get("stock_codes") or sector.get("codes") or []) if str(code or "").strip()]
        item = compute_style_sector_change(name=name, codes=codes, quotes_by_code=quotes_by_code, trade_date=trade_date)
        snapshots.append(item)
    snapshots.sort(key=lambda row: (row["change_pct"] is None, -(row["change_pct"] or -9999), row["name"]))
    return snapshots


def compute_style_sector_change(
    *,
    name: str,
    codes: list[str],
    quotes_by_code: dict[str, dict[str, Any]],
    trade_date: date,
) -> dict[str, Any]:
    pcts: list[float] = []
    for code in codes:
        quote = _quote_for_code(quotes_by_code, code)
        if not quote:
            continue
        pct = _change_pct(quote)
        if pct is not None:
            pcts.append(pct)
    return {
        "tradeDate": trade_date.isoformat(),
        "name": name,
        "change_pct": round(sum(pcts) / len(pcts), 4) if pcts else None,
        "valid_size": len(pcts),
        "sample_size": len(codes),
        "stock_codes": codes,
        "source": "tencent.qt",
    }


def _quote_for_code(quotes_by_code: dict[str, dict[str, Any]], code: str) -> dict[str, Any] | None:
    candidates = [code, code.zfill(6), f"sh{code.zfill(6)}", f"sz{code.zfill(6)}"]
    for key in candidates:
        if key in quotes_by_code:
            return quotes_by_code[key]
    return None


def _change_pct(quote: dict[str, Any]) -> float | None:
    direct = _to_float(quote.get("change_pct") or quote.get("changePct"))
    if direct is not None:
        return direct
    last = _to_float(quote.get("last_price") or quote.get("lastPrice") or quote.get("close"))
    pre_close = _to_float(quote.get("pre_close") or quote.get("pre_close_price") or quote.get("previous_close"))
    if last is None or pre_close is None or pre_close <= 0:
        return None
    return (last - pre_close) / pre_close * 100.0


def _to_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None
