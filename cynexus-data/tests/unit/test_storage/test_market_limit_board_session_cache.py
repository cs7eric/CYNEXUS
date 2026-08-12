from datetime import datetime, timezone
import json

from cynexus_data.storage.redis.market_limit_board_session import MarketLimitBoardSessionCache


class _Pipeline:
    def __init__(self, client):
        self.client = client

    def rpush(self, key, value):
        self.client.data.setdefault(key, []).append(value)
        return self

    def sadd(self, key, *values):
        self.client.data.setdefault(key, set()).update(values)
        return self

    def hset(self, key, mapping=None, **kwargs):
        self.client.data.setdefault(key, {}).update(mapping or kwargs)
        return self

    def expire(self, key, ttl):
        self.client.expirations[key] = ttl
        return self

    def execute(self):
        return []


class _Redis:
    def __init__(self):
        self.data = {}
        self.expirations = {}

    def pipeline(self, transaction=True):
        return _Pipeline(self)

    def hgetall(self, key):
        return self.data.get(key, {})

    def smembers(self, key):
        return self.data.get(key, set())

    def lrange(self, key, start, end):
        return self.data.get(key, [])[start:] if end == -1 else self.data.get(key, [])[start:end + 1]

    def delete(self, *keys):
        deleted = 0
        for key in keys:
            if key in self.data:
                deleted += 1
                self.data.pop(key)
        return deleted

    def exists(self, key):
        return int(key in self.data)


def test_session_cache_compresses_batches_and_round_trips_datetimes():
    redis = _Redis()
    cache = MarketLimitBoardSessionCache(redis, ttl_seconds=43200)
    captured_at = datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc)
    rows = [{"code": "000001", "captured_at": captured_at, "sealed_amount": 1100000}] * 30

    cache.append_samples(
        trade_date="2026-08-11",
        trade_session="morning",
        samples=rows,
        seen={"000001": "up"},
        discovered_at=captured_at,
    )

    key = "cynexus:market-indicator:limit-board:v1:samples:2026-08-11:morning"
    assert isinstance(redis.data[key][0], str)
    assert len(redis.data[key][0]) < len(json.dumps(rows, default=str))
    assert cache.seen_directions(trade_date="2026-08-11", trade_session="morning") == {"000001": "up"}
    assert cache.read_samples(trade_date="2026-08-11", trade_session="morning")[0]["captured_at"] == captured_at
    assert cache.metadata(trade_date="2026-08-11", trade_session="morning")["last_discovery_at"] == captured_at.isoformat()
    assert redis.expirations[key] == 43200


def test_session_cache_deletes_all_session_keys_after_successful_flush():
    redis = _Redis()
    cache = MarketLimitBoardSessionCache(redis)
    cache.append_samples(
        trade_date="2026-08-11",
        trade_session="afternoon",
        samples=[{"code": "000002", "captured_at": datetime(2026, 8, 11, 5, 0, tzinfo=timezone.utc)}],
        seen={"000002": "down"},
    )

    deleted = cache.delete_session(trade_date="2026-08-11", trade_session="afternoon")

    assert deleted == 4
    assert not cache.has_session(trade_date="2026-08-11", trade_session="afternoon")
