from __future__ import annotations

from dataclasses import dataclass
from datetime import date

import requests

from cynexus_data.sources.sina.futures_position import fetch_dce_position_rows, fetch_latest_position_date


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

    def fetch_dce_position_rows(self, trade_date, *, varieties=None) -> list[dict]:
        return fetch_dce_position_rows(
            self.session,
            trade_date,
            varieties=varieties,
            base_url=self.base_url,
            timeout=self.timeout,
        )

    def fetch_latest_futures_position_trade_date(
        self, end_date: date | None = None, *, varieties=None, lookback_days: int = 10
    ) -> date | None:
        return fetch_latest_position_date(
            self.session, end_date=end_date, varieties=varieties,
            lookback_days=lookback_days, base_url=self.base_url, timeout=self.timeout,
        )
