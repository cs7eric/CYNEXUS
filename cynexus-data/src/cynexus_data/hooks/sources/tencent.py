from __future__ import annotations

from cynexus_data.hooks.http import BaseHttpHook
from cynexus_data.sources.tencent.client import TencentClient


class TencentHook(BaseHttpHook):
    def get_client(self) -> TencentClient:
        return TencentClient(session=self.get_session(), base_url=self.base_url, timeout=float(self.connection.timeout or 10))

