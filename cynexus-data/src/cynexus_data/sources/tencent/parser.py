from __future__ import annotations

from datetime import date, datetime
from typing import Any


def parse_tencent_kline_rows(rows: list[list[str]]) -> list[dict]:
    out = []
    for row in rows:
        if len(row) < 6:
            continue
        trade_time = datetime.strptime(str(row[0]), "%Y-%m-%d")
        out.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": float(row[1] or 0),
                "close": float(row[2] or 0),
                "high": float(row[3] or 0),
                "low": float(row[4] or 0),
                "volume": float(row[5] or 0),
                "source": "tencent",
            }
        )
    return out


def parse_tencent_minute_query_rows(rows: list[str], *, trade_date: str | None = None) -> list[dict]:
    out: list[dict] = []
    resolved_date = _parse_trade_date(trade_date) if trade_date else date.today()
    previous_price: float | None = None
    previous_volume: float = 0.0
    previous_amount: float = 0.0
    for row in rows:
        parts = str(row or "").split()
        if len(parts) < 2:
            continue
        try:
            trade_time = datetime.strptime(f"{resolved_date:%Y%m%d}{parts[0]}", "%Y%m%d%H%M")
            price = float(parts[1])
            cumulative_volume = float(parts[2]) if len(parts) > 2 else 0.0
            cumulative_amount = float(parts[3]) if len(parts) > 3 else 0.0
        except (TypeError, ValueError):
            continue
        if price <= 0:
            continue
        open_price = previous_price if previous_price and previous_price > 0 else price
        out.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": open_price,
                "close": price,
                "high": max(open_price, price),
                "low": min(open_price, price),
                "volume": max(cumulative_volume - previous_volume, 0.0),
                "amount": max(cumulative_amount - previous_amount, 0.0),
                "source": "tencent.minute_query.derived",
            }
        )
        previous_price = price
        previous_volume = cumulative_volume
        previous_amount = cumulative_amount
    return out


def _parse_trade_date(value: str | None) -> date:
    text = str(value or "").strip()
    for fmt in ("%Y-%m-%d", "%Y%m%d"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    return date.today()


def parse_qt_stock_fund_flow_text(text: str, *, fetched_at: datetime | None = None) -> dict[str, dict[str, Any]]:
    main_by_symbol: dict[str, dict[str, Any]] = {}
    disk_by_symbol: dict[str, dict[str, Any]] = {}
    captured = (fetched_at or datetime.now()).isoformat(timespec="seconds")
    for raw_line in text.splitlines():
        if "=" not in raw_line:
            continue
        key, value = raw_line.split("=", 1)
        key = key.strip()
        if key.startswith("v_s_pk"):
            symbol = key[len("v_s_pk") :].lower()
            parsed_disk = _parse_qt_stock_disk_ratios(value)
            if parsed_disk:
                disk_by_symbol[symbol] = parsed_disk
        elif key.startswith("v_") and not key.startswith("v_s_"):
            symbol = key[2:].lower()
            parsed_main = _parse_qt_stock_quote_line(value, fetched_at=captured)
            if parsed_main:
                main_by_symbol[symbol] = parsed_main

    out: dict[str, dict[str, Any]] = {}
    for symbol, row in main_by_symbol.items():
        outer = row.get("outer_disc_lots") or 0.0
        inner = row.get("inside_dish_lots") or 0.0
        total = row.get("volume_lots") or 0.0
        active_net_lots = outer - inner
        last_price = row.get("last_price") or 0.0
        row["active_net_lots"] = active_net_lots
        row["active_net_amount_wan"] = active_net_lots * last_price * 100 / 10000
        row["active_buy_ratio"] = outer / total if total else None
        row["active_sell_ratio"] = inner / total if total else None
        row["disk"] = disk_by_symbol.get(symbol, {})
        row["source"] = "qt.gtimg.cn main + s_pk"
        out[symbol] = row
    return out


def parse_qt_realtime_snapshot_text(text: str, *, fetched_at: datetime | None = None) -> dict[str, dict[str, Any]]:
    captured = (fetched_at or datetime.now()).isoformat(timespec="seconds")
    out: dict[str, dict[str, Any]] = {}
    for raw_line in text.splitlines():
        if "=" not in raw_line:
            continue
        key, value = raw_line.split("=", 1)
        key = key.strip()
        if not key.startswith("v_") or key.startswith("v_s_"):
            continue
        symbol = key[2:].lower()
        parsed = _parse_qt_stock_quote_line(value, fetched_at=captured)
        if not parsed:
            continue
        parsed["symbol"] = symbol
        parsed["source"] = "qt.gtimg.cn"
        out[symbol] = parsed
    return out


def summarize_qt_realtime_snapshots(rows: list[dict[str, Any]]) -> dict[str, Any]:
    total_amount_yi = 0.0
    total_volume_wan_lots = 0.0
    rising_count = 0
    falling_count = 0
    flat_count = 0
    limit_up_count = 0
    limit_down_count = 0
    for row in rows:
        change_pct = row.get("change_pct")
        if change_pct is None:
            flat_count += 1
        elif change_pct > 0:
            rising_count += 1
        elif change_pct < 0:
            falling_count += 1
        else:
            flat_count += 1
        if change_pct is not None and change_pct >= 9.5:
            limit_up_count += 1
        if change_pct is not None and change_pct <= -9.5:
            limit_down_count += 1
        amount_wan = row.get("amount_wan") or 0.0
        volume_lots = row.get("volume_lots") or 0.0
        total_amount_yi += amount_wan / 10000
        total_volume_wan_lots += volume_lots / 10000
    return {
        "totalAmount": round(total_amount_yi, 2),
        "totalVolume": round(total_volume_wan_lots, 2),
        "risingCount": rising_count,
        "fallingCount": falling_count,
        "flatCount": flat_count,
        "limitUpCount": limit_up_count,
        "limitDownCount": limit_down_count,
        "stockCount": len(rows),
        "source": "tencent.qt",
    }


def _parse_qt_stock_quote_line(value: str, *, fetched_at: str) -> dict[str, Any] | None:
    parts = _clean_qt_value(value).split("~")
    if len(parts) < 50:
        return None
    code = (parts[2] or "").strip()
    if not code:
        return None
    quote_time = (parts[30] or "").strip()
    quote_trade_date = None
    if len(quote_time) >= 8 and quote_time[:8].isdigit():
        quote_trade_date = datetime.strptime(quote_time[:8], "%Y%m%d").date().isoformat()
    return {
        "code": code,
        "name": (parts[1] or "").strip() or None,
        "last_price": _to_float(parts[3]),
        "pre_close": _to_float(parts[4]),
        "open": _to_float(parts[5]),
        "volume_lots": _to_float(parts[6]),
        "outer_disc_lots": _to_float(parts[7]),
        "inside_dish_lots": _to_float(parts[8]),
        "trade_date": quote_trade_date,
        "quote_time": quote_time or None,
        "change": _to_float(parts[31]),
        "change_pct": _to_float(parts[32]),
        "high": _to_float(parts[33]),
        "low": _to_float(parts[34]),
        "amount_wan": _to_float(parts[37]),
        "turnover_rate": _to_float(parts[38]),
        "pe": _to_float(parts[39]),
        "amplitude": _to_float(parts[43]),
        "float_market_cap_yi": _to_float(parts[44]),
        "total_market_cap_yi": _to_float(parts[45]),
        "pb": _to_float(parts[46]),
        "fetched_at": fetched_at,
    }


def _parse_qt_stock_disk_ratios(value: str) -> dict[str, float | None] | None:
    parts = _clean_qt_value(value).split("~")
    if len(parts) < 4:
        return None
    return {
        "buy_big_ratio": _to_float(parts[0]),
        "buy_small_ratio": _to_float(parts[1]),
        "sell_big_ratio": _to_float(parts[2]),
        "sell_small_ratio": _to_float(parts[3]),
    }


def _to_float(value: Any) -> float | None:
    if value is None:
        return None
    text = str(value).replace(",", "").replace("%", "").strip()
    if text in {"", "--", "-"}:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def _clean_qt_value(value: str) -> str:
    text = value.strip()
    if text.endswith(";"):
        text = text[:-1]
    return text.strip().strip('"')
