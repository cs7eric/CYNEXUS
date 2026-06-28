from __future__ import annotations

from cynexus_data.connections.models import Connection
from cynexus_data.connections.validators import validate_connection


def connection_from_mapping(conn_id: str, values: dict) -> Connection:
    connection = Connection.from_mapping(conn_id, values)
    validate_connection(connection)
    return connection

