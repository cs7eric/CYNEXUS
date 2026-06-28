from __future__ import annotations

from datetime import datetime
from typing import Any, Protocol


class MarketOverviewClient(Protocol):
    def get_a_share_codes_all(self): ...
    def get_quote(self, codes): ...


def create_eltdx_market_overview_client(timeout: float = 20.0):
    try:
        from eltdx import TdxClient  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for live market overview capture") from exc
    return TdxClient(timeout=timeout)


def fetch_eltdx_market_overview(client: MarketOverviewClient, *, captured_at: datetime | None = None) -> dict[str, Any]:
    codes = client.get_a_share_codes_all()
    quotes = client.get_quote(codes)
    now = captured_at or datetime.now()
    rising = falling = flat = limit_up = limit_down = 0
    total_amount = 0.0
    stock_count = 0
    for quote in quotes or []:
        stock_count += 1
        total_amount += float(_get_value(quote, "amount", default=0) or 0)
        last = _get_value(quote, "last_price", "lastPrice")
        prev = _get_value(quote, "pre_close_price", "preClose")
        if last is None or prev in (None, 0):
            continue
        pct = (float(last) - float(prev)) / float(prev) * 100
        if pct > 0:
            rising += 1
        elif pct < 0:
            falling += 1
        else:
            flat += 1
        if pct >= 9.5:
            limit_up += 1
        elif pct <= -9.5:
            limit_down += 1
    return {
        "totalAmount": round(total_amount / 1e8, 2),
        "risingCount": rising,
        "fallingCount": falling,
        "flatCount": flat,
        "limitUpCount": limit_up,
        "limitDownCount": limit_down,
        "stockCount": stock_count,
        "tradingDate": now.date().isoformat(),
        "fetchedAt": now.isoformat(timespec="seconds"),
    }


def _get_value(obj: Any, *names: str, default=None):
    for name in names:
        if isinstance(obj, dict) and name in obj:
            return obj[name]
        if hasattr(obj, name):
            return getattr(obj, name)
    return default
