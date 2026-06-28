from __future__ import annotations

from cynexus_data.hooks.http import BaseHttpHook
from cynexus_data.sources.sina.client import SinaClient


class SinaHook(BaseHttpHook):
    def get_client(self) -> SinaClient:
        return SinaClient(session=self.get_session(), base_url=self.base_url, timeout=float(self.connection.timeout or 10))

