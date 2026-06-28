from __future__ import annotations

from datetime import datetime
from typing import Any


def parse_kline_rows(rows: list[str]) -> list[dict[str, Any]]:
    parsed: list[dict[str, Any]] = []
    for row in rows:
        parts = row.split(",")
        if len(parts) < 7:
            continue
        trade_time = _parse_trade_time(parts[0])
        parsed.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": float(parts[1] or 0),
                "close": float(parts[2] or 0),
                "high": float(parts[3] or 0),
                "low": float(parts[4] or 0),
                "volume": float(parts[5] or 0),
                "amount": float(parts[6] or 0),
                "source": "eastmoney",
            }
        )
    return parsed


def _parse_trade_time(value: str) -> datetime:
    value = value.strip()
    for fmt in ("%Y-%m-%d %H:%M", "%Y-%m-%d"):
        try:
            return datetime.strptime(value, fmt)
        except ValueError:
            continue
    raise ValueError(f"unsupported EastMoney trade time: {value}")

