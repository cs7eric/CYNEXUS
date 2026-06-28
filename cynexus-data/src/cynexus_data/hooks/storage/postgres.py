from __future__ import annotations

from cynexus_data.hooks.database import BaseDatabaseHook
from cynexus_data.storage.postgres.client import PostgresClient


class PostgresHook(BaseDatabaseHook):
    def get_client(self) -> PostgresClient:
        return PostgresClient(url=str(self.connection.url), schema=self.connection.schema or "cynexus_data")

