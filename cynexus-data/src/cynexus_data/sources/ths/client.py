from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import time
from typing import Any

import requests

from cynexus_data.sources.ths.parser import (
    extract_constituents_total_pages,
    extract_total_pages,
    parse_industry_constituents_html,
    parse_industry_fund_flow_html,
)


_FUND_FLOW_PATH = "/funds/hyzjl/cate/3/page/{page}/"
_FUND_FLOW_REFERER = "https://data.10jqka.com.cn/funds/hyzjl/cate/3/"
_FUND_FLOW_HOST = "data.10jqka.com.cn"
_CONSTITUENTS_PATH = "/thshy/detail/code/{code}/page/{page}/"
_CONSTITUENTS_BASE_URL = "https://q.10jqka.com.cn"
_CONSTITUENTS_REFERER = "https://q.10jqka.com.cn/thshy/"
_CONSTITUENTS_HOST = "q.10jqka.com.cn"


@dataclass
class ThsClient:
    session: requests.Session
    base_url: str
    timeout: float = 10

    def fetch_page(self, path: str, headers: dict[str, str] | None = None) -> str:
        response = self.session.get(f"{self.base_url.rstrip('/')}/{path.lstrip('/')}", headers=headers, timeout=self.timeout)
        response.raise_for_status()
        return response.text

    def fetch_industry_fund_flow_page(self, page: int = 1, *, hexin_v: str | None = None) -> dict[str, Any]:
        token = hexin_v or generate_hexin_v()
        html = self.fetch_page_text(_FUND_FLOW_PATH.format(page=page), headers=build_ths_headers(token))
        return {
            "page": page,
            "totalPages": extract_total_pages(html),
            "rows": parse_industry_fund_flow_html(html),
            "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        }

    def fetch_industry_fund_flow_all(self, *, max_pages: int | None = None, page_sleep: float = 0.4) -> dict[str, Any]:
        first = self.fetch_industry_fund_flow_page(1)
        discovered_pages = int(first.get("totalPages") or 0)
        total_pages = discovered_pages or 1
        if max_pages is not None:
            total_pages = min(total_pages, int(max_pages))
        rows = list(first.get("rows") or [])
        page_row_counts = [len(rows)]
        for page in range(2, total_pages + 1):
            time.sleep(page_sleep)
            try:
                payload = self.fetch_industry_fund_flow_page(page)
            except Exception:
                page_row_counts.append(0)
                continue
            page_rows = list(payload.get("rows") or [])
            rows.extend(page_rows)
            page_row_counts.append(len(page_rows))
        rows.sort(key=lambda row: (_sortable(row.get("净额(亿)")), _sortable(row.get("行业指数涨跌幅"))), reverse=True)
        for idx, row in enumerate(rows, start=1):
            row["序号"] = idx
        return {
            "totalPages": discovered_pages,
            "pageRowCounts": page_row_counts,
            "fetchedAt": first.get("fetchedAt"),
            "rowCount": len(rows),
            "rows": rows,
        }

    def fetch_industry_constituents_page(self, code: str, page: int = 1, *, hexin_v: str | None = None) -> dict[str, Any]:
        industry_code = str(code or "").strip()
        if not industry_code:
            raise ValueError("industry code is required")
        token = hexin_v or generate_hexin_v()
        html = self.fetch_url_text(
            f"{_CONSTITUENTS_BASE_URL}{_CONSTITUENTS_PATH.format(code=industry_code, page=page)}",
            headers=build_ths_headers(token, host=_CONSTITUENTS_HOST, referer=_CONSTITUENTS_REFERER),
        )
        return {
            "code": industry_code,
            "page": page,
            "totalPages": extract_constituents_total_pages(html),
            "rows": parse_industry_constituents_html(html),
            "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        }

    def fetch_industry_constituents_all(
        self,
        code: str,
        *,
        max_pages: int | None = None,
        page_sleep: float = 0.4,
    ) -> dict[str, Any]:
        first = self.fetch_industry_constituents_page(code, 1)
        discovered_pages = int(first.get("totalPages") or 1)
        total_pages = discovered_pages
        if max_pages is not None:
            total_pages = min(total_pages, int(max_pages))
        rows = list(first.get("rows") or [])
        page_row_counts = [len(rows)]
        for page in range(2, total_pages + 1):
            time.sleep(page_sleep)
            try:
                payload = self.fetch_industry_constituents_page(code, page)
            except Exception:
                page_row_counts.append(0)
                continue
            page_rows = list(payload.get("rows") or [])
            rows.extend(page_rows)
            page_row_counts.append(len(page_rows))
        deduped = _dedupe_constituent_rows(rows)
        for idx, row in enumerate(deduped, start=1):
            row["序号"] = idx
        return {
            "code": str(code or "").strip(),
            "totalPages": discovered_pages,
            "pageRowCounts": page_row_counts,
            "fetchedAt": first.get("fetchedAt"),
            "rowCount": len(deduped),
            "rows": deduped,
        }

    def fetch_page_text(self, path: str, headers: dict[str, str] | None = None) -> str:
        response = self.session.get(f"{self.base_url.rstrip('/')}/{path.lstrip('/')}", headers=headers, timeout=self.timeout)
        response.raise_for_status()
        return response.content.decode("gbk", errors="replace")

    def fetch_url_text(self, url: str, headers: dict[str, str] | None = None) -> str:
        response = self.session.get(url, headers=headers, timeout=self.timeout)
        response.raise_for_status()
        return response.content.decode("gbk", errors="replace")


def build_ths_headers(hexin_v: str, *, host: str = _FUND_FLOW_HOST, referer: str = _FUND_FLOW_REFERER) -> dict[str, str]:
    return {
        "Accept": "text/html,*/*;q=0.01",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Cache-Control": "no-cache",
        "Connection": "keep-alive",
        "hexin-v": hexin_v,
        "Host": host,
        "Pragma": "no-cache",
        "Referer": referer,
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        ),
    }


def generate_hexin_v() -> str:
    try:
        from akshare.datasets import get_ths_js
        from py_mini_racer import py_mini_racer
    except ImportError as exc:  # pragma: no cover - optional live dependency
        raise RuntimeError("akshare and py-mini-racer are required for live THS fund-flow requests") from exc
    engine = py_mini_racer.MiniRacer()
    with open(get_ths_js("ths.js"), encoding="utf-8") as fp:
        engine.eval(fp.read())
    return str(engine.call("v"))


def _sortable(value: Any) -> float:
    if value is None:
        return float("-inf")
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except ValueError:
        return float("-inf")


def _dedupe_constituent_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: set[str] = set()
    out: list[dict[str, Any]] = []
    for row in rows:
        code = str(row.get("代码") or "").strip()
        if code and code in seen:
            continue
        if code:
            seen.add(code)
        out.append(row)
    return out
