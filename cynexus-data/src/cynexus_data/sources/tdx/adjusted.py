from __future__ import annotations

from datetime import datetime
from typing import Any, Protocol

import pandas as pd


class AdjustedKlineClient(Protocol):
    def get_adjusted_kline_all(self, period: str, code: str, adjust: str, page_size: int = 800, max_pages: int = 200): ...


def create_eltdx_client(timeout: int = 8):
    try:
        from eltdx import TdxClient  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for adjusted daily kline sync") from exc
    return TdxClient(timeout=timeout)


def stock_symbol_to_tdx_code(symbol: str, target_type: str | None = None) -> str:
    code = symbol[-6:] if "." in symbol else symbol
    if target_type == "index":
        if code.startswith(("000", "880")):
            return f"sh{code}"
        if code.startswith("399"):
            return f"sz{code}"
        return f"sh{code}"
    return f"sh{code}" if code.startswith(("5", "6", "9")) else f"sz{code}"


def fetch_adjusted_daily_frame(
    client: AdjustedKlineClient,
    symbol: str,
    adjust: str,
    *,
    start_date=None,
    end_date=None,
    page_size: int = 800,
    max_pages: int = 200,
) -> pd.DataFrame:
    if adjust not in {"qfq", "hfq"}:
        raise ValueError("adjust must be qfq or hfq")
    tdx_code = stock_symbol_to_tdx_code(symbol)
    response = client.get_adjusted_kline_all("day", tdx_code, adjust=adjust, page_size=page_size, max_pages=max_pages)
    bars = getattr(response, "bars", None)
    if bars is None:
        bars = getattr(response, "items", response)
    rows = [_bar_to_record(symbol[-6:] if "." in symbol else symbol, tdx_code, adjust, bar) for bar in bars or []]
    frame = pd.DataFrame(rows)
    if frame.empty:
        return _empty_frame()
    if start_date is not None:
        start = pd.to_datetime(start_date).date()
        frame = frame[frame["trade_date"] >= start]
    if end_date is not None:
        end = pd.to_datetime(end_date).date()
        frame = frame[frame["trade_date"] <= end]
    if frame.empty:
        return _empty_frame()
    return frame.sort_values("trade_date")


def _bar_to_record(symbol: str, tdx_code: str, adjust: str, bar: Any) -> dict[str, Any]:
    trade_time = _get_value(bar, "time", "datetime", "date", "trade_time")
    if isinstance(trade_time, datetime):
        trade_date = trade_time.date()
    else:
        trade_date = pd.to_datetime(trade_time).date()
    market = tdx_code[:2].upper()
    return {
        "market": market,
        "symbol": symbol,
        "code": symbol,
        "tdx_code": tdx_code,
        "trade_date": trade_date,
        "open": float(_get_value(bar, "open")),
        "high": float(_get_value(bar, "high")),
        "low": float(_get_value(bar, "low")),
        "close": float(_get_value(bar, "close")),
        "amount": float(_get_value(bar, "amount", default=0.0) or 0.0),
        "volume": int(_get_value(bar, "volume_lots", "volume", "vol", default=0) or 0),
        "adj_factor": float(_get_value(bar, "adj_factor", "factor", default=1.0) or 1.0),
        "adjust": adjust,
        "source": "eltdx",
        "ingested_at": datetime.now(),
    }


def _get_value(obj: Any, *names: str, default=None):
    for name in names:
        if isinstance(obj, dict) and name in obj:
            return obj[name]
        if hasattr(obj, name):
            return getattr(obj, name)
    return default


def _empty_frame() -> pd.DataFrame:
    return pd.DataFrame(
        columns=[
            "market",
            "symbol",
            "code",
            "tdx_code",
            "trade_date",
            "open",
            "high",
            "low",
            "close",
            "amount",
            "volume",
            "adj_factor",
            "adjust",
            "source",
            "ingested_at",
        ]
    )
