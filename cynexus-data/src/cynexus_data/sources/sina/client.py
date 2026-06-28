from __future__ import annotations

from dataclasses import dataclass

import requests


@dataclass
class SinaClient:
    session: requests.Session
    base_url: str
    timeout: float = 10

    def fetch_minute_kline_raw(self, symbol: str, scale: int = 1, datalen: int = 500) -> list[dict]:
        response = self.session.get(
            f"{self.base_url}/quotes_service/api/json_v2.php/CN_MarketData.getKLineData",
            params={"symbol": symbol, "scale": scale, "ma": "no", "datalen": datalen},
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

