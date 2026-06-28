from __future__ import annotations

from cynexus_data.config.settings import load_yaml_config, resolve_env_template
from cynexus_data.connections.models import Connection


def load_connections() -> dict[str, Connection]:
    raw = resolve_env_template(load_yaml_config("connections.yaml"))
    entries = raw.get("connections") or {}
    return {
        conn_id: Connection.from_mapping(conn_id, values or {})
        for conn_id, values in entries.items()
    }

