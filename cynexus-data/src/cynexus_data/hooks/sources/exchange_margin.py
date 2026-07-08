from __future__ import annotations

from cynexus_data.hooks.http import BaseHttpHook
from cynexus_data.sources.exchange_margin.client import ExchangeMarginClient


class ExchangeMarginHook(BaseHttpHook):
    def get_client(self) -> ExchangeMarginClient:
        return ExchangeMarginClient(
            session=self.get_session(),
            timeout=float(self.connection.timeout or 15.0),
            retry=int(self.connection.retry or 3),
        )
