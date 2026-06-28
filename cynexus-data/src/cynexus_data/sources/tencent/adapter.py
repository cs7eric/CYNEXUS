from __future__ import annotations

from cynexus_data.sources.tencent.parser import parse_tencent_kline_rows


def adapt_kline_payload(payload: dict, code: str, key: str = "qfqday") -> list[dict]:
    data = (payload.get("data") or {}).get(code) or {}
    rows = data.get(key) or data.get("day") or []
    return parse_tencent_kline_rows(rows)

