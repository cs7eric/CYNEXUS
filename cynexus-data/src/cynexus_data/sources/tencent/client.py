from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import requests

from cynexus_data.core.market_symbol import normalize_market_symbol
from cynexus_data.sources.tencent.parser import parse_qt_stock_fund_flow_text


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
