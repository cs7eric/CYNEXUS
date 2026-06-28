from __future__ import annotations

import os
from typing import Any, Sequence

import clickhouse_connect

from cynexus_data.config.settings import ClickHouseConfig


class ClickHouseClient:
    def __init__(self, config: ClickHouseConfig) -> None:
        self.config = config
        _ensure_local_no_proxy(config.host)
        self._client = clickhouse_connect.get_client(
            host=config.host,
            port=config.port,
            username=config.username,
            password=config.password,
            database=config.database,
            secure=config.secure,
            send_receive_timeout=config.send_receive_timeout,
            http_proxy="",
            https_proxy="",
        )

    def query_rows(self, sql: str, parameters: Sequence[Any] | dict[str, Any] | None = None) -> list[tuple[Any, ...]]:
        return list(self._client.query(sql, parameters=parameters).result_rows)

    def query_one(self, sql: str, parameters: Sequence[Any] | dict[str, Any] | None = None) -> tuple[Any, ...] | None:
        rows = self.query_rows(sql, parameters)
        return rows[0] if rows else None

    def command(self, sql: str, parameters: Sequence[Any] | dict[str, Any] | None = None, **kwargs: Any) -> Any:
        return self._client.command(sql, parameters=parameters, **kwargs)

    def insert(self, table: str, rows: Sequence[Sequence[Any]], column_names: Sequence[str], **kwargs: Any) -> Any:
        if not rows:
            return None
        return self._client.insert(table, data=list(rows), column_names=list(column_names), **kwargs)

    def insert_df(self, table: str, frame, column_names: Sequence[str] | None = None, **kwargs: Any) -> Any:
        return self._client.insert_df(table, frame, column_names=column_names, **kwargs)

    def insert_arrow(self, table: str, arrow_table, **kwargs: Any) -> Any:
        return self._client.insert_arrow(table, arrow_table, **kwargs)

    def health(self) -> dict[str, Any]:
        row = self.query_one("SELECT version()")
        return {"ok": True, "database": "clickhouse", "version": row[0] if row else None}


def _ensure_local_no_proxy(host: str) -> None:
    if host not in {"127.0.0.1", "localhost", "::1"}:
        return
    additions = ["127.0.0.1", "localhost", "::1"]
    for env_name in ["NO_PROXY", "no_proxy"]:
        existing = os.getenv(env_name, "")
        parts = [part.strip() for part in existing.split(",") if part.strip()]
        for addition in additions:
            if addition not in parts:
                parts.append(addition)
        os.environ[env_name] = ",".join(parts)

