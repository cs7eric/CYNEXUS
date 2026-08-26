from __future__ import annotations

import base64
import json
import zlib
from datetime import date, datetime
from typing import Any, Iterable


class MarketLimitBoardSessionCache:
    """Keep compressed best-quote samples for one trading session in Redis."""

    schema_version = 1

    def __init__(
        self,
        client: Any,
        *,
        prefix: str = "cynexus:market-indicator:limit-board:v1",
        ttl_seconds: int = 43200,
    ) -> None:
        self.client = client
        self.prefix = prefix.rstrip(":")
        self.ttl_seconds = max(3600, int(ttl_seconds))

    def append_samples(
        self,
        *,
        trade_date: date | str,
        trade_session: str,
        samples: Iterable[dict[str, Any]],
        seen: dict[str, str] | None = None,
        discovered_at: datetime | None = None,
        source: str = "eltdx.quotes",
    ) -> int:
        day = _trade_day(trade_date)
        session = _session(trade_session)
        rows = [dict(row) for row in samples]
        keys = self._keys(day, session)
        pipe = self.client.pipeline(transaction=True)
        if rows:
            pipe.rpush(keys["samples"], _encode_batch(rows))
        if seen:
            pipe.sadd(keys["seen"], *sorted(seen))
            pipe.hset(keys["directions"], mapping={code: direction for code, direction in seen.items()})
        meta = {
            "trade_date": day,
            "trade_session": session,
            "source": source,
        }
        if rows:
            latest = max(
                (row.get("captured_at") for row in rows if isinstance(row.get("captured_at"), datetime)),
                default=None,
            )
            if latest is not None:
                meta["last_capture_at"] = _datetime_text(latest)
        if discovered_at is not None:
            meta["last_discovery_at"] = _datetime_text(discovered_at)
        pipe.hset(keys["meta"], mapping=meta)
        for key in keys.values():
            pipe.expire(key, self.ttl_seconds)
        pipe.execute()
        return len(rows)

    def publish_discoveries(
        self,
        *,
        trade_date: date | str,
        trade_session: str,
        discovered: dict[str, str],
        discovered_at: datetime,
        source: str,
    ) -> int:
        """Publish boards discovered by the whole-market monitor.

        Discovery is intentionally separated from the five-second sampler:
        the former owns the expensive whole-market scan, while the latter only
        follows this bounded code set.
        """
        valid = {
            str(code).strip()[-6:].zfill(6): str(direction)
            for code, direction in discovered.items()
            if str(direction) in {"up", "down"}
        }
        if not valid:
            return 0
        day = _trade_day(trade_date)
        session = _session(trade_session)
        keys = self._keys(day, session)
        pipe = self.client.pipeline(transaction=True)
        pipe.sadd(keys["seen"], *sorted(valid))
        pipe.hset(keys["directions"], mapping=valid)
        pipe.hset(
            keys["meta"],
            mapping={
                "trade_date": day,
                "trade_session": session,
                "source": source,
                "last_discovery_at": _datetime_text(discovered_at),
                "discovery_owner": "market_indicator_refresh",
            },
        )
        for key in keys.values():
            pipe.expire(key, self.ttl_seconds)
        pipe.execute()
        return len(valid)

    def set_session_state(
        self,
        *,
        trade_date: date | str,
        trade_session: str,
        state: str,
        at: datetime,
    ) -> None:
        if state not in {"active", "completed"}:
            raise ValueError(f"unsupported limit-board session state: {state}")
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        self.client.hset(
            keys["meta"],
            mapping={"session_state": state, "session_heartbeat_at": _datetime_text(at)},
        )
        self.client.expire(keys["meta"], self.ttl_seconds)

    def is_session_active(
        self,
        *,
        trade_date: date | str,
        trade_session: str,
        now: datetime,
        stale_after_seconds: int = 90,
    ) -> bool:
        metadata = self.metadata(trade_date=trade_date, trade_session=trade_session)
        if metadata.get("session_state") != "active":
            return False
        heartbeat = metadata.get("session_heartbeat_at")
        if not heartbeat:
            return True
        try:
            heartbeat_at = datetime.fromisoformat(heartbeat.replace("Z", "+00:00"))
        except ValueError:
            return True
        return (now - heartbeat_at).total_seconds() <= max(30, int(stale_after_seconds))

    def seen_directions(self, *, trade_date: date | str, trade_session: str) -> dict[str, str]:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        values = self.client.hgetall(keys["directions"])
        return {_text(code): _text(direction) for code, direction in values.items()}

    def seen_codes(self, *, trade_date: date | str, trade_session: str) -> set[str]:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        return {_text(code) for code in self.client.smembers(keys["seen"])}

    def metadata(self, *, trade_date: date | str, trade_session: str) -> dict[str, str]:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        values = self.client.hgetall(keys["meta"])
        return {_text(key): _text(value) for key, value in values.items()}

    def read_samples(self, *, trade_date: date | str, trade_session: str) -> list[dict[str, Any]]:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        rows: list[dict[str, Any]] = []
        for payload in self.client.lrange(keys["samples"], 0, -1):
            decoded = json.loads(_decode_batch(payload))
            if not isinstance(decoded, list):
                raise ValueError("invalid limit-board Redis sample batch")
            for row in decoded:
                if not isinstance(row, dict):
                    continue
                restored = dict(row)
                if restored.get("captured_at"):
                    restored["captured_at"] = datetime.fromisoformat(str(restored["captured_at"]).replace("Z", "+00:00"))
                rows.append(restored)
        return rows

    def delete_session(self, *, trade_date: date | str, trade_session: str) -> int:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        return int(self.client.delete(*keys.values()))

    def has_session(self, *, trade_date: date | str, trade_session: str) -> bool:
        keys = self._keys(_trade_day(trade_date), _session(trade_session))
        return bool(self.client.exists(keys["samples"]) or self.client.exists(keys["seen"]))

    def _keys(self, trade_date: str, trade_session: str) -> dict[str, str]:
        suffix = f"{trade_date}:{trade_session}"
        return {
            "samples": f"{self.prefix}:samples:{suffix}",
            "seen": f"{self.prefix}:seen:{suffix}",
            "directions": f"{self.prefix}:directions:{suffix}",
            "meta": f"{self.prefix}:meta:{suffix}",
        }


def _encode_batch(rows: list[dict[str, Any]]) -> str:
    payload = json.dumps(
        {"schema_version": MarketLimitBoardSessionCache.schema_version, "rows": rows},
        ensure_ascii=False,
        separators=(",", ":"),
        default=str,
    ).encode("utf-8")
    return base64.b64encode(zlib.compress(payload, level=6)).decode("ascii")


def _decode_batch(value: Any) -> str:
    if isinstance(value, bytes):
        value = value.decode("ascii")
    payload = zlib.decompress(base64.b64decode(str(value))).decode("utf-8")
    decoded = json.loads(payload)
    if not isinstance(decoded, dict) or decoded.get("schema_version") != MarketLimitBoardSessionCache.schema_version:
        raise ValueError("unsupported limit-board Redis sample batch")
    rows = decoded.get("rows")
    return json.dumps(rows, ensure_ascii=False, separators=(",", ":"), default=str)


def _trade_day(value: date | str) -> str:
    return value.isoformat() if isinstance(value, date) else str(value)


def _session(value: str) -> str:
    text = str(value).strip().lower()
    if text not in {"morning", "afternoon"}:
        raise ValueError(f"unsupported trade session: {value}")
    return text


def _datetime_text(value: datetime) -> str:
    return value.isoformat()


def _text(value: Any) -> str:
    return value.decode("utf-8") if isinstance(value, bytes) else str(value)
