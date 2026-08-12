from __future__ import annotations

from typing import Any

from cynexus_data.hooks.base import BaseHook


class RedisHook(BaseHook):
    """Create the shared Redis client without leaking connection config."""

    def get_client(self) -> Any:
        return self._get_client(decode_responses=True)

    def get_binary_client(self) -> Any:
        """Return a byte-preserving client for compressed immutable bundles."""
        return self._get_client(decode_responses=False)

    def _get_client(self, *, decode_responses: bool) -> Any:
        try:
            import redis
        except ImportError as exc:  # pragma: no cover - exercised by runtime image
            raise RuntimeError("redis package is required for market indicator cache") from exc

        extra = self.connection.extra
        return redis.Redis(
            host=str(self.connection.host or "127.0.0.1"),
            port=int(self.connection.port or 6379),
            db=int(extra.get("database") or self.connection.database or 0),
            password=self.connection.password or None,
            socket_timeout=float(self.connection.timeout or 2.0),
            socket_connect_timeout=float(self.connection.timeout or 2.0),
            decode_responses=decode_responses,
        )
