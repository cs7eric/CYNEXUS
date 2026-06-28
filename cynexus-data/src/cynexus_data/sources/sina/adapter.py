from __future__ import annotations

from cynexus_data.sources.sina.parser import parse_sina_minute_rows


def adapt_minute_payload(payload: list[dict]) -> list[dict]:
    return parse_sina_minute_rows(payload)

