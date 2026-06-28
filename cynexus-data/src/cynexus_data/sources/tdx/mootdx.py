from __future__ import annotations

from datetime import datetime
from typing import Any


FREQUENCY_MAP = {
    "1": "1m",
    "1m": "1m",
    "5": "5m",
    "5m": "5m",
    "15": "15m",
    "15m": "15m",
    "30": "30m",
    "30m": "30m",
    "60": "1h",
    "60m": "1h",
    "120": "2h",
    "120m": "2h",
    "101": "day",
    "1d": "day",
    "day": "day",
    "102": "week",
    "1w": "week",
    "week": "week",
}


def create_mootdx_client(*, server: tuple[str, int] | None = None, timeout: int = 10):
    try:
        from mootdx.quotes import StdQuotes
    except ImportError as exc:  # pragma: no cover - optional dependency guard
        raise RuntimeError("mootdx is required for mootdx fallback provider") from exc
    kwargs: dict[str, Any] = {"timeout": timeout, "raise_exception": True}
    if server is not None:
        kwargs["server"] = server
    return StdQuotes(**kwargs)


def fetch_mootdx_kline_rows(
    client,
    *,
    symbol: str,
    period: str,
    adjust: str = "none",
    target_type: str = "stock",
    limit: int = 500,
) -> list[dict[str, Any]]:
    frequency = FREQUENCY_MAP.get(str(period), str(period))
    if _is_minute_frequency(frequency) and adjust not in {"none", "", None}:
        raise ValueError("mootdx minute kline only supports adjust=none")
    kwargs = {"symbol": _plain_symbol(symbol), "frequency": frequency, "offset": limit}
    if adjust in {"qfq", "hfq"} and not _is_minute_frequency(frequency):
        kwargs["adjust"] = adjust
    frame = client.index(**kwargs) if target_type == "index" else client.bars(**kwargs)
    return normalize_mootdx_frame(frame)


def normalize_mootdx_frame(frame) -> list[dict[str, Any]]:
    if frame is None or len(frame) == 0:
        return []
    rows: list[dict[str, Any]] = []
    iterator = frame.iterrows() if hasattr(frame, "iterrows") else enumerate(frame)
    previous_volume: float | None = None
    volume_window: list[float] = []
    for _, row in iterator:
        trade_time = _parse_trade_time(_get(row, "datetime") or _get(row, "date") or _get(row, "time"))
        if trade_time is None:
            continue
        volume = _to_float(_get(row, "volume") or _get(row, "vol")) or 0.0
        volume_ratio = _volume_ratio(volume, previous_volume, volume_window)
        previous_volume = volume
        rows.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": _to_float(_get(row, "open")) or 0.0,
                "close": _to_float(_get(row, "close")) or 0.0,
                "high": _to_float(_get(row, "high")) or 0.0,
                "low": _to_float(_get(row, "low")) or 0.0,
                "volume": volume,
                "amount": _to_float(_get(row, "amount")) or 0.0,
                "volume_ratio": volume_ratio,
                "source": "mootdx",
            }
        )
    return rows


def _plain_symbol(symbol: str) -> str:
    text = str(symbol).strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        return text[2:]
    return text[-6:].zfill(6)


def _is_minute_frequency(frequency: str) -> bool:
    return frequency in {"1m", "5m", "15m", "30m", "1h", "2h"}


def _get(row: Any, key: str) -> Any:
    if isinstance(row, dict):
        return row.get(key)
    getter = getattr(row, "get", None)
    if callable(getter):
        return getter(key)
    return getattr(row, key, None)


def _parse_trade_time(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value
    if value is None:
        return None
    text = str(value).strip()
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M", "%Y-%m-%d"):
        try:
            return datetime.strptime(text, fmt)
        except ValueError:
            continue
    return None


def _to_float(value: Any) -> float | None:
    if value in (None, "", "--", "-"):
        return None
    try:
        return float(str(value).replace(",", "").strip())
    except (TypeError, ValueError):
        return None


def _volume_ratio(volume: float, previous_volume: float | None, window: list[float], *, window_size: int = 5) -> float | None:
    if previous_volume and previous_volume > 0:
        ratio = volume / previous_volume
    elif window:
        baseline = sum(window) / len(window)
        ratio = volume / baseline if baseline > 0 else None
    else:
        ratio = None
    window.append(volume)
    if len(window) > window_size:
        del window[0]
    return round(ratio, 4) if ratio is not None else None
