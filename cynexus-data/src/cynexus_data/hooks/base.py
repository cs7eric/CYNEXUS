from __future__ import annotations

from dataclasses import dataclass

from cynexus_data.connections.models import Connection
from cynexus_data.connections.registry import ConnectionRegistry


@dataclass
class BaseHook:
    conn_id: str
    registry: ConnectionRegistry

    @property
    def connection(self) -> Connection:
        return self.registry.get(self.conn_id)

