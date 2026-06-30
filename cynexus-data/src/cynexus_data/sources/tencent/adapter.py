from __future__ import annotations

from cynexus_data.sources.tencent.parser import parse_tencent_kline_rows, parse_tencent_minute_query_rows


def adapt_kline_payload(payload: dict, code: str, key: str = "qfqday") -> list[dict]:
    data = (payload.get("data") or {}).get(code) or {}
    rows = data.get(key) or data.get("day") or []
    return parse_tencent_kline_rows(rows)


def adapt_minute_query_payload(payload: dict, code: str) -> list[dict]:
    data = (((payload.get("data") or {}).get(code) or {}).get("data") or {})
    rows = data.get("data") or []
    trade_date = _resolve_trade_date(data)
    return parse_tencent_minute_query_rows(rows, trade_date=trade_date)


def _resolve_trade_date(data: dict) -> str | None:
    date_text = str(data.get("date") or "").strip()
    if len(date_text) == 8 and date_text.isdigit():
        return f"{date_text[:4]}-{date_text[4:6]}-{date_text[6:8]}"
    return None
