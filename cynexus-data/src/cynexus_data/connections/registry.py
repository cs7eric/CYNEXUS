from __future__ import annotations

from dataclasses import dataclass, field

from cynexus_data.connections.models import Connection
from cynexus_data.connections.resolver import load_connections


@dataclass
class ConnectionRegistry:
    connections: dict[str, Connection] = field(default_factory=load_connections)

    def get(self, conn_id: str) -> Connection:
        try:
            return self.connections[conn_id]
        except KeyError as exc:
            raise KeyError(f"Connection not found: {conn_id}") from exc

    def list(self) -> list[Connection]:
        return list(self.connections.values())

