from __future__ import annotations

import re
from io import StringIO
from typing import Any


def extract_total_pages(html: str) -> int:
    match = re.search(r'class=["\']page_info["\'][^>]*>\s*(\d+)\s*/\s*(\d+)', html)
    if not match:
        return 0
    return int(match.group(2))


def extract_constituents_total_pages(html: str) -> int:
    total_pages = extract_total_pages(html)
    return total_pages or 1


def parse_industry_fund_flow_html(html: str) -> list[dict[str, Any]]:
    try:
        import pandas as pd
    except ImportError as exc:  # pragma: no cover - dependency guard
        raise RuntimeError("pandas is required to parse THS fund-flow HTML") from exc

    frames = pd.read_html(StringIO(html))
    if not frames:
        return []
    rows = frames[0].dropna(axis=1, how="all").to_dict(orient="records")
    return normalize_fund_flow_rows(rows)


def parse_industry_constituents_html(html: str) -> list[dict[str, Any]]:
    try:
        import pandas as pd
    except ImportError as exc:  # pragma: no cover - dependency guard
        raise RuntimeError("pandas is required to parse THS constituents HTML") from exc

    frames = pd.read_html(StringIO(html))
    if not frames:
        return []
    rows = frames[0].dropna(axis=1, how="all").to_dict(orient="records")
    return normalize_constituent_rows(rows)


def normalize_fund_flow_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    normalized = [normalize_fund_flow_row(row) for row in rows]
    normalized = [row for row in normalized if row]
    normalized.sort(key=lambda row: (_number_for_sort(row.get("净额(亿)")), _number_for_sort(row.get("行业指数涨跌幅"))), reverse=True)
    for idx, row in enumerate(normalized, start=1):
        row["序号"] = idx
    return normalized


def normalize_fund_flow_row(row: dict[str, Any]) -> dict[str, Any]:
    cleaned = {str(key).strip(): _to_python(value) for key, value in row.items() if str(key).strip() != "序号"}
    if "行业指数" in cleaned:
        cleaned.pop("行业指数", None)
    if "涨跌幅" in cleaned and "行业指数涨跌幅" not in cleaned:
        cleaned["行业指数涨跌幅"] = cleaned.pop("涨跌幅")
    if "涨跌幅.1" in cleaned and "领涨股涨跌幅" not in cleaned:
        cleaned["领涨股涨跌幅"] = cleaned.pop("涨跌幅.1")
    return cleaned if cleaned.get("行业") or cleaned.get("industry") or cleaned.get("sector_name") else {}


def normalize_constituent_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    normalized = [normalize_constituent_row(row) for row in rows]
    return [row for row in normalized if row]


def normalize_constituent_row(row: dict[str, Any]) -> dict[str, Any]:
    cleaned = {str(key).strip(): _to_python(value) for key, value in row.items()}
    code = _normalize_code6(cleaned.get("代码"))
    if code is not None:
        cleaned["代码"] = code
    return cleaned if cleaned.get("代码") or cleaned.get("名称") else {}


def _normalize_code6(value: Any) -> str | None:
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


def _number_for_sort(value: Any) -> float:
    if value is None or value == "":
        return float("-inf")
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except ValueError:
        return float("-inf")


def _to_python(value: Any) -> Any:
    if value is None:
        return None
    try:
        if hasattr(value, "item"):
            value = value.item()
    except Exception:
        pass
    if isinstance(value, float) and value != value:
        return None
    return value
