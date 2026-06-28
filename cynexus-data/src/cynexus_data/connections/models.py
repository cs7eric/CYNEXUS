from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Connection:
    conn_id: str
    type: str
    host: str | None = None
    port: int | None = None
    database: str | None = None
    username: str | None = None
    password: str | None = None
    url: str | None = None
    path: str | None = None
    root_path: str | None = None
    base_url: str | None = None
    timeout: int | float | None = None
    retry: int | None = None
    schema: str | None = None
    extra: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, conn_id: str, values: dict[str, Any]) -> "Connection":
        known = {
            "type",
            "host",
            "port",
            "database",
            "username",
            "password",
            "url",
            "path",
            "root_path",
            "base_url",
            "timeout",
            "retry",
            "schema",
        }
        kwargs = {key: values.get(key) for key in known}
        if kwargs.get("port") not in (None, ""):
            kwargs["port"] = int(kwargs["port"])
        if kwargs.get("retry") not in (None, ""):
            kwargs["retry"] = int(kwargs["retry"])
        if kwargs.get("timeout") not in (None, ""):
            kwargs["timeout"] = float(kwargs["timeout"])
        extra = {key: value for key, value in values.items() if key not in known}
        return cls(conn_id=conn_id, extra=extra, **kwargs)

