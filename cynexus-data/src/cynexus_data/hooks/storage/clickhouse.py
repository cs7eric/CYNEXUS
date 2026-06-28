from __future__ import annotations

from cynexus_data.config.settings import ClickHouseConfig
from cynexus_data.hooks.database import BaseDatabaseHook
from cynexus_data.storage.clickhouse.client import ClickHouseClient


class ClickHouseHook(BaseDatabaseHook):
    def get_config(self) -> ClickHouseConfig:
        conn = self.connection
        return ClickHouseConfig(
            host=conn.host or "127.0.0.1",
            port=int(conn.port or 28123),
            database=conn.database or "cynexus",
            username=conn.username or "cynexus",
            password=conn.password or "",
            send_receive_timeout=int(conn.timeout or 1800),
        )

    def get_client(self) -> ClickHouseClient:
        return ClickHouseClient(self.get_config())

