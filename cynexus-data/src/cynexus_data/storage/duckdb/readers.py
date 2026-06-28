from __future__ import annotations

from cynexus_data.storage.duckdb.client import DuckDBClient


def table_count(client: DuckDBClient, table: str) -> int:
    with client.connect(read_only=True) as con:
        return int(con.execute(f"SELECT count(*) FROM {table}").fetchone()[0])

