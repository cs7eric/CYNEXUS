from __future__ import annotations

from dataclasses import dataclass
from dataclasses import field
import time
from typing import Any

import requests

from cynexus_data.sources.eastmoney.browser_session import browser_session


@dataclass
class EastMoneyClient:
    session: requests.Session
    base_url: str
    timeout: float = 10
    last_market_fund_flow_metadata: dict[str, Any] = field(default_factory=dict, init=False)

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

    def fetch_market_fund_flow(self, *, limit: int = 120) -> list[dict[str, Any]]:
        params = _market_fund_flow_params(limit)
        headers = {"Referer": "https://data.eastmoney.com/zjlx/dpzjlx.html"}
        endpoint = "https://push2his.eastmoney.com/api/qt/stock/fflow/daykline/get"
        try:
            rows = self._fetch_market_fund_flow_http(endpoint, params=params, headers=headers)
            self.last_market_fund_flow_metadata = {
                "fallback_used": False,
                "source_mode": "http",
                "stale": False,
                "error": None,
            }
            return rows
        except Exception as http_error:  # noqa: BLE001 - browser context is the bounded source fallback.
            try:
                payload = browser_session.request_json(
                    endpoint,
                    params=params,
                    headers=headers,
                )
                rows = parse_market_fund_flow_rows((payload.get("data") or {}).get("klines") or [])
                if not rows:
                    raise RuntimeError("EastMoney browser-context endpoint returned no rows")
                self.last_market_fund_flow_metadata = {
                    "fallback_used": True,
                    "source_mode": "browser_context",
                    "stale": False,
                    "error": None,
                    "http_error": str(http_error),
                }
                return rows
            except Exception as browser_error:  # noqa: BLE001 - surface both attempts to the job alert.
                error = f"HTTP request failed: {http_error}; browser-context request failed: {browser_error}"
                self.last_market_fund_flow_metadata = {
                    "fallback_used": True,
                    "source_mode": "failed",
                    "stale": True,
                    "error": error,
                }
                raise RuntimeError(error) from browser_error

    def _fetch_market_fund_flow_http(
        self,
        endpoint: str,
        *,
        params: dict[str, Any],
        headers: dict[str, str],
    ) -> list[dict[str, Any]]:
        """Fetch and parse market fund flow through the configured HTTP session."""
        if isinstance(self.session, requests.Session):
            from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient

            response = EastMoneyHttpClient(
                session=self.session,
                min_interval=0.0,
                jitter=(0.0, 0.0),
                timeout=self.timeout,
            ).get(endpoint, params=params, headers=headers, timeout=self.timeout)
        else:
            response = self.session.get(
                endpoint,
                params=params,
                headers=headers,
                timeout=self.timeout,
            )
        response.raise_for_status()
        payload = response.json()
        rows = parse_market_fund_flow_rows((payload.get("data") or {}).get("klines") or [])
        if not rows:
            raise RuntimeError("EastMoney market fund-flow endpoint returned no rows")
        return rows

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


def _market_fund_flow_params(limit: int) -> dict[str, Any]:
    return {
        "lmt": str(max(0, int(limit))),
        "klt": "101",
        "secid": "1.000001",
        "secid2": "0.399001",
        "fields1": "f1,f2,f3,f7",
        "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65",
        "ut": "b2884a393a59ad64002292a3e90d46a5",
        "_": int(time.time() * 1000),
    }


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


def parse_market_fund_flow_rows(rows: list[Any]) -> list[dict[str, Any]]:
    parsed: list[dict[str, Any]] = []
    for raw in rows:
        parts = str(raw).split(",")
        if len(parts) < 15:
            continue
        parsed.append(
            {
                "trade_date": parts[0],
                "main_net_inflow": _to_float(parts[1]),
                "small_net_inflow": _to_float(parts[2]),
                "medium_net_inflow": _to_float(parts[3]),
                "large_net_inflow": _to_float(parts[4]),
                "super_large_net_inflow": _to_float(parts[5]),
                "main_net_inflow_ratio": _to_float(parts[6]),
                "small_net_inflow_ratio": _to_float(parts[7]),
                "medium_net_inflow_ratio": _to_float(parts[8]),
                "large_net_inflow_ratio": _to_float(parts[9]),
                "super_large_net_inflow_ratio": _to_float(parts[10]),
                "sh_close": _to_float(parts[11]),
                "sh_change_pct": _to_float(parts[12]),
                "sz_close": _to_float(parts[13]),
                "sz_change_pct": _to_float(parts[14]),
            }
        )
    return parsed


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
