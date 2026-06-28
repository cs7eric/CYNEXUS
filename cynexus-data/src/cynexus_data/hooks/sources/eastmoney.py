from __future__ import annotations

from cynexus_data.hooks.http import BaseHttpHook
from cynexus_data.sources.eastmoney.client import EastMoneyClient


class EastMoneyHook(BaseHttpHook):
    def get_client(self) -> EastMoneyClient:
        return EastMoneyClient(session=self.get_session(), base_url=self.base_url, timeout=float(self.connection.timeout or 10))

