from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class CheckpointStore:
    _client: Any | None = None
    _items: dict[str, dict[str, Any]] = field(default_factory=dict)

    @classmethod
    def memory(cls) -> "CheckpointStore":
        return cls()

    @classmethod
    def postgres(cls, client: Any) -> "CheckpointStore":
        return cls(_client=client)

    def get(self, key: str) -> dict[str, Any] | None:
        if self._client is not None:
            return self._client.get_checkpoint(key)
        return self._items.get(key)

    def put(self, key: str, job_key: str, value: dict[str, Any]) -> None:
        if self._client is not None:
            self._client.upsert_checkpoint(key, job_key, value)
        self._items[key] = value

