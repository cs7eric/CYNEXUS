from __future__ import annotations

from datetime import date, datetime, time
from typing import Any

from cynexus_data.sources.tdx.adjusted import stock_symbol_to_tdx_code


def create_eltdx_trade_client(timeout: float = 20.0):
    try:
        from eltdx import TdxClient  # type: ignore
    except Exception as exc:  # pragma: no cover - optional live dependency
        raise RuntimeError("eltdx is required for historical trade details") from exc
    return TdxClient(timeout=timeout)


def fetch_history_trade_rows(
    symbol: str,
    trade_date: str | date,
    *,
    target_type: str | None = None,
    page_size: int = 2000,
    max_pages: int = 100,
) -> list[dict[str, Any]]:
    """Fetch the complete historical TDX trade-detail series for one symbol/day.

    eltdx returns minute-resolution trade-detail records (not Level-2 matching
    orders).  The helper deliberately uses the paginated ``get_trades_all`` API
    so a short first page cannot silently truncate the trading session.
    """
    trading_date = _coerce_date(trade_date)
    code = _plain_code(symbol)
    tdx_code = stock_symbol_to_tdx_code(code, target_type=target_type)
    client = create_eltdx_trade_client()
    try:
        ticks = _fetch_all_ticks(
            client,
            tdx_code,
            trading_date,
            page_size=page_size,
            max_pages=max_pages,
        )
        rows = [_normalize_tick(tick, code, tdx_code, trading_date, target_type=target_type) for tick in ticks]
        rows = [row for row in rows if _is_trading_time(row["time_label"])]
        return sorted(rows, key=lambda row: (_time_sort_key(row["time_label"]), row.get("absolute_index") or 0))
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()


def _fetch_all_ticks(client: Any, code: str, trading_date: date, *, page_size: int, max_pages: int) -> list[Any]:
    """Read every page, including pages shorter than the requested size.

    TDX servers commonly cap a historical response at 1,800 rows even when
    ``count=2000`` is requested.  ``eltdx.get_trades_all`` treats that short
    page as the terminal page, which drops the morning session.  The protocol
    actually signals completion with an empty page, so continue until empty.
    """
    ticks: list[Any] = []
    start = 0
    for _ in range(max_pages):
        page = client.get_trades(
            code,
            date=trading_date,
            start=start,
            count=page_size,
        )
        page_ticks = list(getattr(page, "ticks", ()) or ())
        if not page_ticks:
            break
        ticks.extend(page_ticks)
        # The server may cap each response at 1,800 rows regardless of the
        # requested count; advance by the number actually returned.
        start += len(page_ticks)
    return ticks


def _normalize_tick(
    tick: Any,
    code: str,
    tdx_code: str,
    trading_date: date,
    *,
    target_type: str | None = None,
) -> dict[str, Any]:
    value = _object_dict(tick)
    label = str(value.get("time_label") or value.get("time") or "")
    dt = value.get("trade_datetime")
    if not isinstance(dt, datetime):
        parsed = _parse_time(label)
        dt = datetime.combine(trading_date, parsed) if parsed else None
    raw_price = _number(value.get("price"))
    # TDX encodes ETF historical trade prices one decimal place too high
    # (e.g. 588170: 10.15 instead of 1.015).  The quote protocol confirms
    # the ETF scale, so normalize before calculating amount and rendering.
    price = raw_price * 0.1 if raw_price is not None and _is_etf(code, target_type) else raw_price
    volume = _integer(value.get("volume"))
    side = _normalize_side(value.get("side"))
    amount = price * volume * 100 if price is not None and volume is not None else None
    return {
        "source": "eltdx.history_trade",
        "symbol": code,
        "code": code,
        "tdx_code": tdx_code,
        "trade_date": trading_date.isoformat(),
        "trade_datetime": dt.isoformat() if isinstance(dt, datetime) else None,
        "trade_time": label,
        "time": label,
        "time_label": label,
        "price": price,
        "volume": volume,
        "vol": volume,
        "amount": amount,
        "buy_sell": side,
        "side": side,
        "order_count": _integer(value.get("order_count")),
        "index": _integer(value.get("index")),
        "absolute_index": _integer(value.get("absolute_index")),
    }


def _object_dict(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return value
    raw = getattr(value, "__dict__", None)
    if isinstance(raw, dict):
        return raw
    return {name: getattr(value, name) for name in dir(value) if not name.startswith("_") and not callable(getattr(value, name, None))}


def _coerce_date(value: str | date) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _parse_time(value: str) -> time | None:
    text = value.strip()
    for fmt in ("%H:%M:%S", "%H:%M"):
        try:
            return datetime.strptime(text, fmt).time()
        except ValueError:
            continue
    return None


def _plain_code(value: str) -> str:
    text = str(value or "").strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        text = text[2:]
    if "." in text:
        text = text.split(".", 1)[0]
    return text[-6:].zfill(6)


def _is_etf(code: str, target_type: str | None) -> bool:
    if target_type and target_type.lower() == "etf":
        return True
    return code.startswith(("15", "16", "18", "50", "51", "56", "58"))


def _number(value: Any) -> float | None:
    try:
        return None if value in (None, "", "--", "-") else float(value)
    except (TypeError, ValueError):
        return None


def _integer(value: Any) -> int | None:
    try:
        return None if value in (None, "", "--", "-") else int(value)
    except (TypeError, ValueError):
        return None


def _normalize_side(value: Any) -> str:
    text = str(value or "").upper()
    if text in {"B", "BUY", "1", "主动买", "买盘"}:
        return "BUY"
    if text in {"S", "SELL", "2", "主动卖", "卖盘"}:
        return "SELL"
    return "NEUTRAL"


def _is_trading_time(value: str) -> bool:
    try:
        parsed = _parse_time(value)
        if parsed is None:
            return False
        minute = parsed.hour * 60 + parsed.minute
        return (570 <= minute <= 690) or (780 <= minute <= 900)
    except (TypeError, ValueError):
        return False


def _time_sort_key(value: str) -> int:
    parsed = _parse_time(value)
    return parsed.hour * 60 + parsed.minute if parsed else 0
