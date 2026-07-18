from __future__ import annotations

import os
import hashlib
import threading
import time
from typing import Any


_EPOCH_MS = 1704067200000  # 2024-01-01T00:00:00Z
_LOCK = threading.Lock()
_LAST_MS = 0
_SEQUENCE = 0


def next_bigint_id() -> int:
    global _LAST_MS, _SEQUENCE
    worker = int(os.getenv("CYNEXUS_DATA_WORKER_ID", "7")) & 0x3FF
    with _LOCK:
        now_ms = int(time.time() * 1000)
        if now_ms == _LAST_MS:
            _SEQUENCE = (_SEQUENCE + 1) & 0xFFF
            if _SEQUENCE == 0:
                while now_ms <= _LAST_MS:
                    now_ms = int(time.time() * 1000)
        else:
            _SEQUENCE = 0
        _LAST_MS = now_ms
        return ((now_ms - _EPOCH_MS) << 22) | (worker << 12) | _SEQUENCE


def stable_bigint_id(namespace: str, *parts: Any) -> int:
    payload = "|".join([namespace, *(str(part) for part in parts)])
    digest = hashlib.blake2b(payload.encode("utf-8"), digest_size=8).digest()
    return int.from_bytes(digest, "big") & 0x7FFF_FFFF_FFFF_FFFF
