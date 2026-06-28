from __future__ import annotations

from datetime import datetime
from typing import Any


def parse_sina_minute_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    previous_volume: float | None = None
    volume_window: list[float] = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        try:
            trade_time = _parse_trade_time(str(row.get("day") or ""))
            volume = _to_float(row.get("volume")) or 0.0
            volume_ratio = _volume_ratio(volume, previous_volume, volume_window)
            previous_volume = volume
            out.append(
                {
                    "trade_time": trade_time,
                    "trade_date": trade_time.date(),
                    "open": _to_float(row.get("open")) or 0.0,
                    "high": _to_float(row.get("high")) or 0.0,
                    "low": _to_float(row.get("low")) or 0.0,
                    "close": _to_float(row.get("close")) or 0.0,
                    "volume": volume,
                    "amount": _to_float(row.get("amount")) or 0.0,
                    "volume_ratio": volume_ratio,
                    "source": "sina",
                }
            )
        except ValueError:
            continue
    return out


def _parse_trade_time(value: str) -> datetime:
    text = value.strip()
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"):
        try:
            return datetime.strptime(text, fmt)
        except ValueError:
            continue
    raise ValueError(f"unsupported Sina trade time: {value}")


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
