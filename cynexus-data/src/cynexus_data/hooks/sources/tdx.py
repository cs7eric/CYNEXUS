from __future__ import annotations

from pathlib import Path

from cynexus_data.hooks.base import BaseHook
from cynexus_data.sources.tdx.market_overview import create_eltdx_market_overview_client


class TdxHook(BaseHook):
    def get_root_path(self) -> Path:
        return Path(str(self.connection.root_path or "")).expanduser()

    def get_market_overview_client(self, timeout: float = 20.0):
        return create_eltdx_market_overview_client(timeout=timeout)
