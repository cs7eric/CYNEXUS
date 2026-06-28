from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Any


def build_industry_heatmap_payload(
    rows: list[dict[str, Any]],
    *,
    snapshot_date: date,
    top_n: int = 200,
    source: str = "tdx_industry_quote_snapshot",
    fetched_at: str | None = None,
) -> dict[str, Any]:
    items = [_industry_item(row) for row in rows]
    items = [item for item in items if item is not None]
    items.sort(key=lambda item: (-(item.get("amount") or 0), item.get("name") or ""))
    total_items = len(items)
    if top_n and top_n > 0:
        items = items[:top_n]
    return {
        "ok": True,
        "kind": "industries",
        "kinds": ["行业"],
        "snapshotDate": snapshot_date.isoformat(),
        "items": items,
        "totalItems": len(items),
        "itemsTotal": total_items,
        "topN": top_n,
        "totalStocks": sum(_int_or_zero(item.get("stockCount")) for item in items),
        "hiddenStocks": 0,
        "hiddenNoQuote": 0,
        "hiddenEmptySectors": 0,
        "fetchedAt": fetched_at or datetime.now(timezone.utc).isoformat(),
        "source": source,
    }


def _industry_item(row: dict[str, Any]) -> dict[str, Any] | None:
    name = _text(row.get("name") or row.get("sector_name") or row.get("sectorName"))
    code = _text(row.get("full_code") or row.get("sectorCode") or row.get("code") or row.get("code6"))
    code6 = _text(row.get("code6") or (code[-6:] if code else None))
    if not name or not code6:
        return None
    full_code = code if code and len(code) > 6 else _infer_full_code(code6)
    change_pct = _float_or_none(row.get("change_pct") or row.get("changePercent"))
    amount = _float_or_none(row.get("amount") or row.get("value")) or 0.0
    total_hand = _float_or_none(row.get("total_hand") or row.get("volume"))
    stock_count = _int_or_none(row.get("stock_count") or row.get("stockCount"))
    return {
        "name": name,
        "sectorCode": full_code,
        "fullCode": full_code,
        "code6": code6,
        "kind": "industries",
        "kindLabel": "行业 (通达信)",
        "topicId": [code6],
        "value": amount,
        "changePercent": change_pct,
        "amount": amount,
        "stockCount": stock_count or 0,
        "risingCount": 1 if change_pct is not None and change_pct > 0 else 0,
        "fallingCount": 1 if change_pct is not None and change_pct < 0 else 0,
        "flatCount": 1 if change_pct is None or abs(change_pct) < 0.0001 else 0,
        "limitUpCount": 1 if change_pct is not None and change_pct >= 9.9 else 0,
        "latestPrice": _float_or_none(row.get("last_price") or row.get("latestPrice")),
        "preClosePrice": _float_or_none(row.get("pre_close_price") or row.get("preClosePrice")),
        "openPrice": _float_or_none(row.get("open_price") or row.get("openPrice")),
        "highPrice": _float_or_none(row.get("high_price") or row.get("highPrice")),
        "lowPrice": _float_or_none(row.get("low_price") or row.get("lowPrice")),
        "change": _float_or_none(row.get("change")),
        "volume": total_hand,
        "turnoverRate": _float_or_none(row.get("turnover_rate") or row.get("turnoverRate")),
        "amplitude": _float_or_none(row.get("amplitude")),
        "children": [],
    }


def _infer_full_code(code6: str) -> str:
    return f"sh{code6}"


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _float_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _int_or_none(value: Any) -> int | None:
    parsed = _float_or_none(value)
    return int(parsed) if parsed is not None else None


def _int_or_zero(value: Any) -> int:
    parsed = _int_or_none(value)
    return parsed if parsed is not None else 0
