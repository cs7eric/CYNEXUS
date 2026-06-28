from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import requests


@dataclass
class EastMoneyClient:
    session: requests.Session
    base_url: str
    timeout: float = 10

    def fetch_kline_raw(self, secid: str, klt: str = "101", fqt: str = "1", limit: int = 500) -> dict:
        response = self.session.get(
            f"{self.base_url}/api/qt/stock/kline/get",
            params={
                "fields1": "f1,f2,f3,f4,f5,f6",
                "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
                "ut": "fa5fd1943c7b386f172d6893dbfba10b",
                "secid": secid,
                "klt": klt,
                "fqt": fqt,
                "lmt": str(limit),
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def fetch_stock_meta(self, code: str) -> dict[str, Any]:
        last_error: Exception | None = None
        for secid in eastmoney_secid_candidates(code):
            try:
                payload = self.fetch_stock_meta_raw(secid)
                data = payload.get("data") or {}
                if data:
                    return parse_stock_meta(data, fallback_code=code)
            except (requests.RequestException, ValueError) as exc:
                last_error = exc
                continue
        if last_error:
            raise RuntimeError(f"EastMoney stock meta request failed for {code}: {last_error}") from last_error
        raise RuntimeError(f"EastMoney returned empty stock meta for {code}")

    def fetch_stock_meta_raw(self, secid: str) -> dict[str, Any]:
        response = self.session.get(
            f"{self.base_url}/api/qt/stock/get",
            params={
                "secid": secid,
                "fields": "f57,f58,f20,f21,f100",
                "ut": "fa5fd1943c7b386f172d6893dbfba10b",
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def fetch_market_breadth(
        self,
        *,
        page_size: int = 80,
        max_pages: int = 70,
        page_sleep: float = 0.0,
    ) -> dict[str, Any]:
        rows: list[dict[str, Any]] = []
        total_pages_seen = 0
        for page in range(1, max_pages + 1):
            payload = self.fetch_clist_page(page=page, page_size=page_size)
            data = payload.get("data") or {}
            page_rows = list(data.get("diff") or [])
            if not page_rows:
                break
            rows.extend(page_rows)
            total_pages_seen = page
            if len(page_rows) < page_size:
                break
            if page_sleep > 0:
                import time

                time.sleep(page_sleep)
        if not rows:
            raise RuntimeError("EastMoney clist returned no market breadth rows")
        return summarize_market_breadth(rows, pages=total_pages_seen)

    def fetch_clist_page(self, *, page: int = 1, page_size: int = 80) -> dict[str, Any]:
        response = self.session.get(
            f"{self.base_url}/api/qt/clist/get",
            params={
                "pn": str(page),
                "pz": str(page_size),
                "po": "1",
                "np": "1",
                "ut": "bd1d9ddb04089700cf9c27f6f7426281",
                "fltt": "2",
                "invt": "2",
                "fid": "f3",
                "fs": "m:0+t:6,m:0+t:13,m:0+t:80,m:1+t:2,m:1+t:23",
                "fields": "f2,f3,f5,f6,f12",
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()


def summarize_market_breadth(rows: list[dict[str, Any]], *, pages: int | None = None) -> dict[str, Any]:
    rising_count = 0
    falling_count = 0
    flat_count = 0
    limit_up_count = 0
    limit_down_count = 0
    total_amount = 0.0
    total_volume = 0.0
    for row in rows:
        change_pct = _to_float(row.get("f3"))
        if change_pct is None:
            flat_count += 1
        elif change_pct > 0:
            rising_count += 1
        elif change_pct < 0:
            falling_count += 1
        else:
            flat_count += 1
        if change_pct is not None and change_pct >= 9.5:
            limit_up_count += 1
        if change_pct is not None and change_pct <= -9.5:
            limit_down_count += 1
        total_volume += _to_float(row.get("f5")) or 0.0
        total_amount += _to_float(row.get("f6")) or 0.0
    return {
        "totalAmount": round(total_amount / 1e8, 2),
        "totalVolume": round(total_volume / 1e4, 2),
        "risingCount": rising_count,
        "fallingCount": falling_count,
        "flatCount": flat_count,
        "limitUpCount": limit_up_count,
        "limitDownCount": limit_down_count,
        "stockCount": len(rows),
        "source": "eastmoney",
        "pages": pages,
    }


def parse_stock_meta(data: dict[str, Any], *, fallback_code: str) -> dict[str, Any]:
    code = str(data.get("f57") or fallback_code).strip()[-6:].zfill(6)
    total_market_cap = _to_float(data.get("f20"))
    float_market_cap = _to_float(data.get("f21"))
    return {
        "code": code,
        "name": str(data.get("f58") or "").strip(),
        "industry": str(data.get("f100") or "").strip() or None,
        "total_market_cap": total_market_cap,
        "float_market_cap": float_market_cap,
        "source": "eastmoney",
    }


def eastmoney_secid_candidates(code: str) -> list[str]:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    value = value[-6:].zfill(6)
    if value.startswith(("5", "6", "9")):
        return [f"1.{value}", f"0.{value}"]
    return [f"0.{value}", f"1.{value}"]


def _to_float(value: Any) -> float | None:
    if value in (None, "", "-", "--"):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None
