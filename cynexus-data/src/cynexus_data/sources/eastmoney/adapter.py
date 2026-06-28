from __future__ import annotations

from cynexus_data.sources.eastmoney.parser import parse_kline_rows


def adapt_kline_payload(payload: dict) -> list[dict]:
    data = payload.get("data") or {}
    rows = data.get("klines") or []
    return parse_kline_rows(rows)

