from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from typing import Any


ZH_TO_CANONICAL = {
    "序号": "rank",
    "行业": "sector_name",
    "板块": "sector_name",
    "code": "sector_code",
    "行业code": "sector_code",
    "行业代码": "sector_code",
    "行业指数涨跌幅": "change_pct",
    "涨跌幅": "change_pct",
    "流入资金(亿)": "inflow",
    "流出资金(亿)": "outflow",
    "净额(亿)": "net",
    "公司家数": "company_count",
    "领涨股": "leader_stock",
    "领涨股涨跌幅": "leader_change",
    "当前价(元)": "leader_price",
}

STOCK_FIELD_ALIASES = {
    "lastPrice": "last_price",
    "preClose": "pre_close",
    "volumeLots": "volume_lots",
    "outerDisc": "outer_disc_lots",
    "insideDish": "inside_dish_lots",
    "amountWan": "amount_wan",
    "turnoverRate": "turnover_rate",
    "floatMarketCapYi": "float_market_cap_yi",
    "totalMarketCapYi": "total_market_cap_yi",
    "activeNetLots": "active_net_lots",
    "activeNetAmountWan": "active_net_amount_wan",
    "activeBuyRatio": "active_buy_ratio",
    "activeSellRatio": "active_sell_ratio",
    "fetchedAt": "fetched_at",
}


def normalize_sector_fund_flow_row(
    row: dict[str, Any],
    *,
    trade_date: date,
    scope: str = "industry",
    source: str = "ths.10jqka.com.cn",
    source_type: str = "crawler",
    captured_at: datetime | None = None,
) -> dict[str, Any] | None:
    canonical = _canonicalize_keys(row)
    sector_name = _strip(canonical.get("sector_name") or canonical.get("industry"))
    if not sector_name:
        return None
    return {
        "scope": scope,
        "trade_date": trade_date,
        "sector_code": _strip(canonical.get("sector_code") or canonical.get("industry_code")),
        "sector_name": sector_name,
        "rank": _to_int(canonical.get("rank")),
        "change_pct": _to_float(canonical.get("change_pct")),
        "inflow": _to_float(canonical.get("inflow")),
        "outflow": _to_float(canonical.get("outflow")),
        "net": _to_float(canonical.get("net")),
        "company_count": _to_int(canonical.get("company_count")),
        "leader_stock": _strip(canonical.get("leader_stock")),
        "leader_change": _to_float(canonical.get("leader_change")),
        "leader_price": _to_float(canonical.get("leader_price")),
        "source": source,
        "source_type": source_type,
        "captured_at": captured_at,
        "extra": dict(row),
    }


def normalize_sector_fund_flow_snapshot(
    rows: list[dict[str, Any]],
    *,
    trade_date: date,
    scope: str = "industry",
    source: str = "ths.10jqka.com.cn",
    source_type: str = "crawler",
    captured_at: datetime | None = None,
) -> list[dict[str, Any]]:
    records = [
        record
        for row in rows
        if isinstance(row, dict)
        for record in [
            normalize_sector_fund_flow_row(
                row,
                trade_date=trade_date,
                scope=scope,
                source=source,
                source_type=source_type,
                captured_at=captured_at,
            )
        ]
        if record is not None
    ]
    records.sort(key=lambda item: (item.get("net") if item.get("net") is not None else float("-inf"), item.get("change_pct") or 0), reverse=True)
    for rank, record in enumerate(records, start=1):
        record["rank"] = rank
    return records


def sector_fund_flow_top_bottom(records: list[dict[str, Any]], top_n: int = 10) -> dict[str, Any]:
    top = records[:top_n]
    bottom = list(reversed(records[-top_n:])) if len(records) >= top_n else list(reversed(records))
    return {"topN": top_n, "top": top, "bottom": bottom, "count": len(records)}


def normalize_stock_fund_flow_row(
    row: dict[str, Any],
    *,
    trade_date: date,
    source: str = "tencent.qt",
    source_type: str = "crawler",
    captured_at: datetime | None = None,
) -> dict[str, Any] | None:
    canonical = _canonicalize_stock_keys(row)
    code = _strip(canonical.get("code"))
    if not code:
        return None
    disk = canonical.get("disk") if isinstance(canonical.get("disk"), dict) else {}
    fetched_at = _parse_datetime(canonical.get("fetched_at")) or captured_at
    return {
        "trade_date": trade_date,
        "code": code[-6:].zfill(6),
        "symbol": _strip(canonical.get("symbol")),
        "name": _strip(canonical.get("name")),
        "last_price": _to_float(canonical.get("last_price")),
        "pre_close": _to_float(canonical.get("pre_close")),
        "open": _to_float(canonical.get("open")),
        "high": _to_float(canonical.get("high")),
        "low": _to_float(canonical.get("low")),
        "change": _to_float(canonical.get("change")),
        "change_pct": _to_float(canonical.get("change_pct")),
        "volume_lots": _to_float(canonical.get("volume_lots")),
        "amount_wan": _to_float(canonical.get("amount_wan")),
        "turnover_rate": _to_float(canonical.get("turnover_rate")),
        "outer_disc_lots": _to_float(canonical.get("outer_disc_lots")),
        "inside_dish_lots": _to_float(canonical.get("inside_dish_lots")),
        "active_net_lots": _to_float(canonical.get("active_net_lots")),
        "active_net_amount_wan": _to_float(canonical.get("active_net_amount_wan")),
        "active_buy_ratio": _to_float(canonical.get("active_buy_ratio")),
        "active_sell_ratio": _to_float(canonical.get("active_sell_ratio")),
        "buy_big_ratio": _to_float(canonical.get("buy_big_ratio") or disk.get("buy_big_ratio") or disk.get("buyBigRatio")),
        "buy_small_ratio": _to_float(canonical.get("buy_small_ratio") or disk.get("buy_small_ratio") or disk.get("buySmallRatio")),
        "sell_big_ratio": _to_float(canonical.get("sell_big_ratio") or disk.get("sell_big_ratio") or disk.get("sellBigRatio")),
        "sell_small_ratio": _to_float(canonical.get("sell_small_ratio") or disk.get("sell_small_ratio") or disk.get("sellSmallRatio")),
        "pe": _to_float(canonical.get("pe")),
        "pb": _to_float(canonical.get("pb")),
        "amplitude": _to_float(canonical.get("amplitude")),
        "float_market_cap_yi": _to_float(canonical.get("float_market_cap_yi")),
        "total_market_cap_yi": _to_float(canonical.get("total_market_cap_yi")),
        "source": source,
        "source_type": source_type,
        "captured_at": fetched_at or captured_at,
        "extra": dict(row),
    }


def normalize_stock_fund_flow_snapshot(
    rows: list[dict[str, Any]],
    *,
    trade_date: date,
    source: str = "tencent.qt",
    source_type: str = "crawler",
    captured_at: datetime | None = None,
) -> list[dict[str, Any]]:
    records = [
        record
        for row in rows
        if isinstance(row, dict)
        for record in [
            normalize_stock_fund_flow_row(
                row,
                trade_date=trade_date,
                source=source,
                source_type=source_type,
                captured_at=captured_at,
            )
        ]
        if record is not None
    ]
    records.sort(key=lambda item: item.get("active_net_amount_wan") if item.get("active_net_amount_wan") is not None else float("-inf"), reverse=True)
    return records


def _canonicalize_keys(row: dict[str, Any]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in row.items():
        normalized_key = ZH_TO_CANONICAL.get(str(key).strip(), str(key).strip())
        out[normalized_key] = value
    return out


def _canonicalize_stock_keys(row: dict[str, Any]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in row.items():
        normalized_key = STOCK_FIELD_ALIASES.get(str(key).strip(), str(key).strip())
        out[normalized_key] = value
    return out


def _to_float(value: Any) -> float | None:
    if value is None or value == "":
        return None
    if isinstance(value, Decimal):
        return float(value)
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).replace(",", "").replace("%", "").strip()
    if text in {"", "--", "-"}:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def _to_int(value: Any) -> int | None:
    parsed = _to_float(value)
    return int(parsed) if parsed is not None else None


def _strip(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _parse_datetime(value: Any) -> datetime | None:
    if value in (None, ""):
        return None
    if isinstance(value, datetime):
        return value
    try:
        return datetime.fromisoformat(str(value))
    except ValueError:
        return None
