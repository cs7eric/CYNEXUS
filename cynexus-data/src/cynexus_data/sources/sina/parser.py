from __future__ import annotations

from datetime import datetime


def parse_sina_minute_rows(rows: list[dict]) -> list[dict]:
    out = []
    for row in rows:
        trade_time = datetime.strptime(str(row.get("day")), "%Y-%m-%d %H:%M:%S")
        out.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": float(row.get("open") or 0),
                "high": float(row.get("high") or 0),
                "low": float(row.get("low") or 0),
                "close": float(row.get("close") or 0),
                "volume": float(row.get("volume") or 0),
                "amount": float(row.get("amount") or 0),
                "source": "sina",
            }
        )
    return out

