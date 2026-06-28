from __future__ import annotations

from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient


def upsert_by_key(client: PostgresClient, table: str, key: str, values: dict[str, Any]) -> None:
    columns = list(values.keys())
    assignments = ", ".join(f"{column}=EXCLUDED.{column}" for column in columns if column != key)
    sql = (
        f"INSERT INTO {client.schema}.{table} ({', '.join(columns)}) "
        f"VALUES ({', '.join(':' + column for column in columns)}) "
        f"ON CONFLICT ({key}) DO UPDATE SET {assignments}"
    )
    with client.session() as db:
        db.execute(text(sql), values)

