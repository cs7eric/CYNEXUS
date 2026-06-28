from __future__ import annotations

from datetime import datetime
from typing import Any, Iterable, Sequence


DEFAULT_GROUP_KEYS = ("market", "symbol", "source")


def aggregate_ohlcv(rows: Iterable[dict[str, Any]], interval_minutes: int, group_keys: Sequence[str] = DEFAULT_GROUP_KEYS) -> list[dict[str, Any]]:
    if interval_minutes <= 1:
        return [dict(row) for row in rows]
    buckets: dict[tuple[Any, ...], dict[str, Any]] = {}
    for row in sorted(rows, key=lambda item: tuple(item.get(key) for key in group_keys) + (item["trade_time"],)):
        dt = row["trade_time"]
        if not isinstance(dt, datetime):
            raise TypeError("trade_time must be a datetime")
        bucket_dt = dt.replace(minute=(dt.minute // interval_minutes) * interval_minutes, second=0, microsecond=0)
        bucket_key = tuple(row.get(key) for key in group_keys) + (bucket_dt,)
        bucket = buckets.get(bucket_key)
        if bucket is None:
            buckets[bucket_key] = {
                **row,
                "trade_time": bucket_dt,
                "period": interval_minutes,
            }
            continue
        bucket["high"] = max(float(bucket["high"]), float(row["high"]))
        bucket["low"] = min(float(bucket["low"]), float(row["low"]))
        bucket["close"] = row["close"]
        bucket["volume"] = float(bucket.get("volume") or 0) + float(row.get("volume") or 0)
        bucket["amount"] = float(bucket.get("amount") or 0) + float(row.get("amount") or 0)
    return list(buckets.values())
