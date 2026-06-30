from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


PACKAGE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = Path(__file__).resolve().parents[3]
CONFIG_DIR = PACKAGE_ROOT / "config"


def load_yaml_config(name: str) -> dict[str, Any]:
    path = CONFIG_DIR / name
    if not path.exists():
        return {}
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def resolve_env_template(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: resolve_env_template(item) for key, item in value.items()}
    if isinstance(value, list):
        return [resolve_env_template(item) for item in value]
    if not isinstance(value, str):
        return value
    if value.startswith("${") and value.endswith("}"):
        expr = value[2:-1]
        name, _, default = expr.partition(":")
        resolved = os.getenv(name)
        return resolved if resolved is not None else resolve_env_template(default)
    return value


@dataclass(frozen=True)
class ClickHouseConfig:
    host: str
    port: int
    database: str
    username: str
    password: str
    secure: bool = False
    send_receive_timeout: int = 1800

    @classmethod
    def from_env(cls) -> "ClickHouseConfig":
        return cls(
            host=os.getenv("CLICKHOUSE_HOST", "127.0.0.1"),
            port=int(os.getenv("CLICKHOUSE_HTTP_PORT", "28123")),
            database=os.getenv("CLICKHOUSE_DATABASE", "cynexus"),
            username=os.getenv("CLICKHOUSE_USER", "cynexus"),
            password=os.getenv("CLICKHOUSE_PASSWORD", ""),
            secure=os.getenv("CLICKHOUSE_SECURE", "false").lower() in {"1", "true", "yes"},
            send_receive_timeout=int(os.getenv("CLICKHOUSE_SEND_RECEIVE_TIMEOUT", "1800")),
        )


@dataclass(frozen=True)
class RuntimeConfig:
    project_root: Path = PROJECT_ROOT
    package_root: Path = PACKAGE_ROOT
    config_dir: Path = CONFIG_DIR
    default_connection_id: str = "postgres_default"
