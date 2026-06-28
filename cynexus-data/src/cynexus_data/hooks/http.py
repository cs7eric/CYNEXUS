from __future__ import annotations

import requests

from cynexus_data.hooks.base import BaseHook


class BaseHttpHook(BaseHook):
    def get_session(self) -> requests.Session:
        session = requests.Session()
        session.headers.update({"User-Agent": "Mozilla/5.0 CynexusData/0.1"})
        return session

    @property
    def base_url(self) -> str:
        return str(self.connection.base_url or "").rstrip("/")

