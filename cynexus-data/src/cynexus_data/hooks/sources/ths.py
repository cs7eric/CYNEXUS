from __future__ import annotations

from cynexus_data.hooks.http import BaseHttpHook
from cynexus_data.sources.ths.client import ThsClient


class ThsHook(BaseHttpHook):
    def get_client(self) -> ThsClient:
        return ThsClient(session=self.get_session(), base_url=self.base_url, timeout=float(self.connection.timeout or 10))

