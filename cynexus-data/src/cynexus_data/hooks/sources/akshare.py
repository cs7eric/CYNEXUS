from __future__ import annotations

from cynexus_data.hooks.base import BaseHook
from cynexus_data.sources.akshare.client import AkshareClient


class AkshareHook(BaseHook):
    def get_client(self) -> AkshareClient:
        return AkshareClient()
