from __future__ import annotations

import json
import zlib
from datetime import date
from typing import Iterable, Mapping


PREFIX = "cynexus:market:timeshare:v1"


def cache_key(trade_date: date, target_type: str, market: str, symbol: str) -> str:
    return f"{PREFIX}:{trade_date.isoformat()}:{target_type}:{market.upper()}:{symbol.upper()}"


def encode_series(rows: Iterable[Mapping[str, object]]) -> bytes:
    payload = {
        "v": 1,
        "points": [
            [
                int(row["slot"]),
                float(row["price"]),
                None if row.get("avg_price") is None else float(row["avg_price"]),
                int(row.get("volume_lots") or row.get("volume") or 0),
                None if row.get("turnover") is None else float(row["turnover"]),
            ]
            for row in rows
        ],
    }
    # The v1 wire contract deliberately uses a dependency-free JSON+zlib
    # bundle.  The leading codec byte leaves room for a future MessagePack /
    # Zstd upgrade without invalidating keys or TTL semantics.
    encoded = json.dumps(payload, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return b"D" + zlib.compress(encoded, level=6)


def publish(redis_client, trade_date: date, target_type: str, market: str, symbol: str, rows, ttl_seconds: int = 604800) -> str:
    key = cache_key(trade_date, target_type, market, symbol)
    redis_client.setex(key, max(1, int(ttl_seconds)), encode_series(rows))
    return key
