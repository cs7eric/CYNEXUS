from __future__ import annotations

from contextlib import contextmanager
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from threading import Event, Thread
from typing import Any, Iterator


@dataclass
class LockManager:
    _client: Any | None = None
    _held: set[str] = field(default_factory=set)

    @classmethod
    def memory(cls) -> "LockManager":
        return cls()

    @classmethod
    def postgres(cls, client: Any) -> "LockManager":
        return cls(_client=client)

    @contextmanager
    def acquire(self, lock_key: str, owner: str, ttl_seconds: int = 900) -> Iterator[bool]:
        acquired = self.try_acquire(lock_key, owner, ttl_seconds)
        stop = Event()
        heartbeat: Thread | None = None
        if acquired and self._client is not None:
            interval = max(5, min(60, ttl_seconds // 3))

            def renew() -> None:
                while not stop.wait(interval):
                    try:
                        expires_at = datetime.now(timezone.utc) + timedelta(seconds=ttl_seconds)
                        self._client.refresh_lock(lock_key, owner, expires_at)
                    except Exception:
                        # The job will still release in finally; a transient
                        # heartbeat failure must not hide the job result.
                        continue

            heartbeat = Thread(target=renew, name=f"cynexus-lock-{owner[:12]}", daemon=True)
            heartbeat.start()
        try:
            yield acquired
        finally:
            if heartbeat is not None:
                stop.set()
                heartbeat.join(timeout=2)
            if acquired:
                self.release(lock_key, owner)

    def try_acquire(self, lock_key: str, owner: str, ttl_seconds: int) -> bool:
        if self._client is not None:
            expires_at = datetime.now(timezone.utc) + timedelta(seconds=ttl_seconds)
            return self._client.try_acquire_lock(lock_key, owner, expires_at)
        if lock_key in self._held:
            return False
        self._held.add(lock_key)
        return True

    def release(self, lock_key: str, owner: str) -> None:
        if self._client is not None:
            self._client.release_lock(lock_key, owner)
        self._held.discard(lock_key)
