from __future__ import annotations

from cynexus_data.hooks.database import BaseDatabaseHook
from cynexus_data.storage.duckdb.client import DuckDBClient


class DuckDBHook(BaseDatabaseHook):
    def get_client(self) -> DuckDBClient:
        return DuckDBClient(path=str(self.connection.path))

