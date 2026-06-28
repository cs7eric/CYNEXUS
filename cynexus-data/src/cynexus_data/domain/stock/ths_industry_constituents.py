from __future__ import annotations

from datetime import date, datetime
from typing import Any


def normalize_industry_constituent_snapshot(
    rows: list[dict[str, Any]],
    *,
    industry_code: str,
    snapshot_date: date,
    source: str = "q.10jqka.com.cn",
    source_type: str = "crawler",
    captured_at: datetime | None = None,
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    seen: set[str] = set()
    for row in rows:
        code = normalize_code6(row.get("代码") or row.get("code") or row.get("stock_code"))
        if not code or code in seen:
            continue
        seen.add(code)
        records.append(
            {
                "snapshot_date": snapshot_date,
                "industry_code": str(industry_code).strip(),
                "stock_code": code,
                "stock_name": _text(row.get("名称") or row.get("name") or row.get("stock_name")),
                "rank": _int(row.get("序号") or row.get("rank")),
                "price": _float(row.get("现价") or row.get("price")),
                "change_pct": _float(row.get("涨跌幅(%)") or row.get("change_pct")),
                "change": _float(row.get("涨跌") or row.get("change")),
                "speed_pct": _float(row.get("涨速(%)") or row.get("speed_pct")),
                "turnover_pct": _float(row.get("换手(%)") or row.get("turnover_pct")),
                "volume_ratio": _float(row.get("量比") or row.get("volume_ratio")),
                "amplitude_pct": _float(row.get("振幅(%)") or row.get("amplitude_pct")),
                "amount_text": _text(row.get("成交额") or row.get("amount_text")),
                "float_shares_text": _text(row.get("流通股") or row.get("float_shares_text")),
                "float_market_cap_text": _text(row.get("流通市值") or row.get("float_market_cap_text")),
                "pe": _float(row.get("市盈率") or row.get("pe")),
                "source": source,
                "source_type": source_type,
                "captured_at": captured_at,
                "extra": {
                    key: value
                    for key, value in row.items()
                    if key
                    not in {
                        "序号",
                        "代码",
                        "名称",
                        "现价",
                        "涨跌幅(%)",
                        "涨跌",
                        "涨速(%)",
                        "换手(%)",
                        "量比",
                        "振幅(%)",
                        "成交额",
                        "流通股",
                        "流通市值",
                        "市盈率",
                    }
                },
            }
        )
    return records


def normalize_code6(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    if "." in text:
        text = text.split(".", 1)[0]
    if text.isdigit():
        return text.zfill(6)
    return text


def _text(value: Any) -> str | None:
    if value in (None, ""):
        return None
    text = str(value).strip()
    return None if text in {"--", "nan", "NaN"} else text


def _float(value: Any) -> float | None:
    if value in (None, "", "--"):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _int(value: Any) -> int | None:
    parsed = _float(value)
    return None if parsed is None else int(parsed)
