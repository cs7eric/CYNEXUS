from __future__ import annotations

from cynexus_data.connections.models import Connection


def validate_connection(connection: Connection) -> None:
    if not connection.type:
        raise ValueError(f"{connection.conn_id}: connection type is required")
    if connection.type == "http" and not connection.base_url:
        raise ValueError(f"{connection.conn_id}: base_url is required")
    if connection.type == "postgres" and not connection.url:
        raise ValueError(f"{connection.conn_id}: url is required")
    if connection.type == "duckdb" and not connection.path:
        raise ValueError(f"{connection.conn_id}: path is required")

