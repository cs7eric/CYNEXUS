from __future__ import annotations

import json
import time
import uuid
from datetime import date, datetime, timezone
from typing import Any, Protocol


class MarketIndicatorRepositoryProtocol(Protocol):
    def save_capture(
        self,
        rows: list[dict[str, Any]],
        events: list[dict[str, Any]],
        industry_rows: list[dict[str, Any]],
        *,
        source: str,
        candidate_rows: list[dict[str, Any]] | None = None,
        captured_at: datetime | None = None,
        transmission: dict[str, Any] | None = None,
    ) -> dict[str, int]: ...


class MarketIndicatorCache:
    """Redis staging and latest-state cache for indicator captures.

    A capture is published as one immutable JSON bundle. The pending sorted set
    is only acknowledged after PostgreSQL persistence succeeds, so retries are
    safe because the PostgreSQL repository uses stable ids and UPSERTs.
    """

    schema_version = 2

    def __init__(self, client: Any, *, prefix: str = "cynexus:market-indicator", ttl_seconds: int = 86400) -> None:
        self.client = client
        self.prefix = prefix
        self.ttl_seconds = max(300, int(ttl_seconds))

    def stage_capture(
        self,
        *,
        captured_at: datetime,
        rows: list[dict[str, Any]],
        events: list[dict[str, Any]],
        industry_rows: list[dict[str, Any]],
        candidate_rows: list[dict[str, Any]],
        source: str,
        transmission: dict[str, Any] | None = None,
        retain_pending: bool = True,
    ) -> dict[str, Any]:
        captured = _utc(captured_at)
        trade_date = captured.date().isoformat()
        capture_id = captured.isoformat()
        bundle = {
            "schema_version": self.schema_version,
            "capture_id": capture_id,
            "trade_date": trade_date,
            "captured_at": capture_id,
            "source": source,
            "rows": rows,
            "events": events,
            "industry_rows": industry_rows,
            "candidate_rows": candidate_rows,
            "transmission": transmission or {},
        }
        capture_key = self._capture_key(trade_date, capture_id)
        pending_key = self._pending_key(trade_date)
        latest_key = self._latest_key(trade_date)
        serialized = json.dumps(bundle, ensure_ascii=False, default=str, separators=(",", ":"))

        pipe = self.client.pipeline(transaction=True)
        pipe.set(capture_key, serialized, ex=self.ttl_seconds)
        if retain_pending:
            pipe.zadd(pending_key, {capture_id: captured.timestamp()})
            pipe.expire(pending_key, self.ttl_seconds)
        pipe.set(latest_key, capture_id, ex=self.ttl_seconds)
        pipe.set(self._latest_trade_date_key(), trade_date, ex=self.ttl_seconds)
        pipe.execute()
        return {
            "capture_id": capture_id,
            "trade_date": trade_date,
            "rows": len(rows),
            "events": len(events),
            "pending_captures": self.pending_count(trade_date) if retain_pending else 0,
        }

    def latest_snapshots(self, trade_date: date) -> dict[str, dict[str, Any]]:
        trade_day = trade_date.isoformat()
        capture_id = self.client.get(self._latest_key(trade_day))
        if not capture_id:
            return {}
        raw = self.client.get(self._capture_key(trade_day, str(capture_id)))
        if not raw:
            return {}
        bundle = _decode_bundle(raw)
        rows = [_restore_datetimes(row, ("captured_at",)) for row in bundle.get("rows", [])]
        return {str(row.get("code")): row for row in rows if row.get("code")}

    def pending_count(self, trade_date: date | str) -> int:
        trade_day = trade_date.isoformat() if isinstance(trade_date, date) else str(trade_date)
        return int(self.client.zcard(self._pending_key(trade_day)))

    def flush_due(
        self,
        repository: MarketIndicatorRepositoryProtocol,
        *,
        min_age_seconds: int = 180,
        now: datetime | None = None,
        max_captures: int = 1,
    ) -> dict[str, Any]:
        """Persist old staged captures and acknowledge them after commit."""

        now_utc = _utc(now or datetime.now(timezone.utc))
        cutoff = now_utc.timestamp() - max(0, int(min_age_seconds))
        lock_key = f"{self.prefix}:flush-lock"
        token = uuid.uuid4().hex
        if not self.client.set(lock_key, token, nx=True, ex=120):
            return {"captures": 0, "snapshots": 0, "events": 0, "industries": 0, "pending_captures": 0, "locked": True}

        result = {"captures": 0, "snapshots": 0, "events": 0, "industries": 0, "pending_captures": 0, "locked": False}
        try:
            pending_keys = self.client.keys(f"{self.prefix}:pending:*")
            for pending_key in pending_keys:
                if result["captures"] >= max(1, int(max_captures)):
                    break
                trade_day = str(pending_key).rsplit(":", 1)[-1]
                capture_ids = self.client.zrangebyscore(pending_key, "-inf", cutoff, start=0, num=max(1, int(max_captures)))
                for capture_id in capture_ids:
                    if result["captures"] >= max(1, int(max_captures)):
                        break
                    raw = self.client.get(self._capture_key(trade_day, str(capture_id)))
                    if not raw:
                        continue
                    bundle = _decode_bundle(raw)
                    captured_at = _parse_datetime(bundle["captured_at"])
                    written = repository.save_capture(
                        [_restore_datetimes(row, ("captured_at",)) for row in bundle.get("rows", [])],
                        [_restore_datetimes(event, ("event_at",)) for event in bundle.get("events", [])],
                        [
                            _restore_datetimes(
                                row,
                                ("captured_at", "first_limit_up_at", "first_event_at"),
                            )
                            for row in bundle.get("industry_rows", [])
                        ],
                        source=str(bundle.get("source") or "eltdx.quotes"),
                        candidate_rows=list(bundle.get("candidate_rows") or []),
                        captured_at=captured_at,
                        transmission=dict(bundle.get("transmission") or {}),
                    )
                    self.client.zrem(pending_key, capture_id)
                    # Keep the atomically published latest bundle available for
                    # live readers; older bundles are already durable in PG.
                    latest_capture_id = self.client.get(self._latest_key(trade_day))
                    if not _same_capture_id(capture_id, latest_capture_id):
                        self.client.delete(self._capture_key(trade_day, str(capture_id)))
                    result["captures"] += 1
                    result["snapshots"] += int(written.get("snapshots") or 0)
                    result["events"] += int(written.get("events") or 0)
                    result["industries"] += int(written.get("industries") or 0)
            result["pending_captures"] = sum(self.pending_count(str(key).rsplit(":", 1)[-1]) for key in pending_keys)
            return result
        finally:
            try:
                self.client.eval(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                    1,
                    lock_key,
                    token,
                )
            except Exception:
                # The lock expires automatically if a Redis implementation does
                # not support EVAL (for example a minimal test double).
                pass

    def _capture_key(self, trade_date: str, capture_id: str) -> str:
        return f"{self.prefix}:capture:{trade_date}:{capture_id}"

    def _pending_key(self, trade_date: str) -> str:
        return f"{self.prefix}:pending:{trade_date}"

    def _latest_key(self, trade_date: str) -> str:
        return f"{self.prefix}:latest:{trade_date}"

    def _latest_trade_date_key(self) -> str:
        return f"{self.prefix}:latest-trade-date"


def _decode_bundle(raw: str | bytes) -> dict[str, Any]:
    value = json.loads(raw)
    if not isinstance(value, dict) or int(value.get("schema_version") or 0) not in {1, MarketIndicatorCache.schema_version}:
        raise ValueError("unsupported market indicator cache bundle")
    return value


def _same_capture_id(left: Any, right: Any) -> bool:
    def normalize(value: Any) -> str:
        if isinstance(value, bytes):
            return value.decode("utf-8")
        return str(value)

    return right is not None and normalize(left) == normalize(right)


def _restore_datetimes(value: dict[str, Any], fields: tuple[str, ...]) -> dict[str, Any]:
    restored = dict(value)
    for field in fields:
        if restored.get(field):
            restored[field] = _parse_datetime(restored[field])
    return restored


def _parse_datetime(value: Any) -> datetime:
    if isinstance(value, datetime):
        return _utc(value)
    text = str(value).replace("Z", "+00:00")
    parsed = datetime.fromisoformat(text)
    return _utc(parsed)


def _utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)
