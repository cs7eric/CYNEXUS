from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

import requests


UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


@dataclass
class BaiduStockClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 10.0

    def kline_with_ma(self, code: str, *, start_time: str = "") -> dict[str, Any]:
        code6 = _plain_code(code)
        response = self.session.get(
            "https://finance.pae.baidu.com/selfselect/getstockquotation",
            params={
                "all": "1",
                "isIndex": "false",
                "isBk": "false",
                "isBlock": "false",
                "isFutures": "false",
                "isStock": "true",
                "newFormat": "1",
                "group": "quotation_kline_ab",
                "finClientType": "pc",
                "code": code6,
                "start_time": start_time,
                "ktype": "1",
            },
            headers={
                "User-Agent": UA,
                "Accept": "application/vnd.finance-web.v1+json",
                "Origin": "https://gushitong.baidu.com",
                "Referer": "https://gushitong.baidu.com/",
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        payload = response.json()
        data = payload.get("Result") or {}
        market_data = (data.get("newMarketData") or {}).get("marketData") or ""
        rows = [row for row in str(market_data).split(";") if row]
        return {"keys": (data.get("newMarketData") or {}).get("keys") or [], "rows": rows, "source": "baidu.selfselect.getstockquotation"}


def _plain_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    if "." in value:
        value = value.split(".", 1)[0]
    return value[-6:].zfill(6)
