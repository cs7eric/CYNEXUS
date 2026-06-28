from __future__ import annotations

from cynexus_data.connections.registry import ConnectionRegistry


def test_connection_registry_loads_yaml_defaults() -> None:
    registry = ConnectionRegistry()
    clickhouse = registry.get("clickhouse_default")
    assert clickhouse.type == "clickhouse"
    assert clickhouse.host
    assert clickhouse.port

