from __future__ import annotations

from typing import Any

from cynexus_data.sources.sina.parser import parse_sina_minute_rows


def adapt_minute_payload(payload: list[dict[str, Any]] | None) -> list[dict]:
    return parse_sina_minute_rows(payload or [])
