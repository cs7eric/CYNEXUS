from __future__ import annotations

from cynexus_data.storage.duckdb.client import DuckDBClient


def execute(client: DuckDBClient, sql: str) -> None:
    with client.connect(read_only=False) as con:
        con.execute(sql)

