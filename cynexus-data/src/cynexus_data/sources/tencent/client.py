from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import requests

from cynexus_data.core.market_symbol import normalize_market_symbol
from cynexus_data.sources.tencent.parser import parse_qt_realtime_snapshot_text, parse_qt_stock_fund_flow_text, summarize_qt_realtime_snapshots


@dataclass
class TencentClient:
    session: requests.Session
    base_url: str
    timeout: float = 10

    def fetch_kline_raw(self, code: str, unit: str = "day", limit: int = 2000, adjust: str = "qfq") -> dict:
        response = self.session.get(
            f"{self.base_url}/appstock/app/fqkline/get",
            params={"param": f"{code},{unit},,,{limit},{adjust}"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def fetch_minute_query_raw(self, code: str) -> dict:
        response = self.session.get(
            f"{self.base_url}/appstock/app/minute/query",
            params={"code": code},
            headers={"Referer": "https://gu.qq.com/"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def fetch_stock_fund_flow_batch(self, symbols: list[str]) -> dict[str, dict[str, Any]]:
        normalized = [normalize_market_symbol(symbol) for symbol in symbols if str(symbol or "").strip()]
        if not normalized:
            return {}
        query_symbols = normalized + [f"s_pk{symbol}" for symbol in normalized]
        response = self.session.get(
            "https://qt.gtimg.cn/q=" + ",".join(query_symbols),
            headers={"Referer": "https://gu.qq.com/"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        text = response.content.decode("gb18030", errors="ignore")
        return parse_qt_stock_fund_flow_text(text)

    def fetch_realtime_snapshots_batch(
        self,
        symbols: list[str],
        *,
        batch_size: int = 500,
        batch_sleep: float = 0.22,
    ) -> dict[str, dict[str, Any]]:
        normalized = [normalize_market_symbol(symbol) for symbol in symbols if str(symbol or "").strip()]
        if not normalized:
            return {}
        import time

        out: dict[str, dict[str, Any]] = {}
        for start in range(0, len(normalized), batch_size):
            batch = normalized[start : start + batch_size]
            response = self.session.get(
                "https://qt.gtimg.cn/q=" + ",".join(batch),
                headers={"Referer": "https://gu.qq.com/"},
                timeout=self.timeout,
            )
            response.raise_for_status()
            text = response.content.decode("gb18030", errors="ignore")
            out.update(parse_qt_realtime_snapshot_text(text))
            if start + batch_size < len(normalized) and batch_sleep > 0:
                time.sleep(batch_sleep)
        return out

    def fetch_market_overview(
        self,
        symbols: list[str],
        *,
        batch_size: int = 500,
        batch_sleep: float = 0.22,
    ) -> dict[str, Any]:
        snapshots = self.fetch_realtime_snapshots_batch(symbols, batch_size=batch_size, batch_sleep=batch_sleep)
        if not snapshots:
            raise RuntimeError("Tencent qt returned no realtime snapshots")
        payload = summarize_qt_realtime_snapshots(list(snapshots.values()))
        payload["requestedCount"] = len(symbols)
        payload["returnedCount"] = len(snapshots)
        return payload
