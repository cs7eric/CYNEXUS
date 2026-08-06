from datetime import datetime, timedelta, timezone

from cynexus_data.storage.redis.market_indicator import MarketIndicatorCache


class FakePipeline:
    def __init__(self, client):
        self.client = client
        self.operations = []

    def set(self, *args, **kwargs):
        self.operations.append((self.client.set, args, kwargs))
        return self

    def zadd(self, *args, **kwargs):
        self.operations.append((self.client.zadd, args, kwargs))
        return self

    def expire(self, *args, **kwargs):
        return self

    def execute(self):
        return [method(*args, **kwargs) for method, args, kwargs in self.operations]


class FakeRedis:
    def __init__(self):
        self.values = {}
        self.zsets = {}

    def pipeline(self, transaction=True):
        assert transaction is True
        return FakePipeline(self)

    def set(self, key, value, ex=None, nx=False):
        if nx and key in self.values:
            return False
        self.values[key] = value
        return True

    def get(self, key):
        return self.values.get(key)

    def zadd(self, key, mapping):
        self.zsets.setdefault(key, {}).update(mapping)
        return len(mapping)

    def zcard(self, key):
        return len(self.zsets.get(key, {}))

    def zrangebyscore(self, key, minimum, maximum, start=0, num=None):
        rows = [(member, score) for member, score in self.zsets.get(key, {}).items() if score <= float(maximum)]
        rows.sort(key=lambda item: item[1])
        members = [member for member, _ in rows]
        return members[start : start + num if num is not None else None]

    def zrem(self, key, member):
        self.zsets.get(key, {}).pop(member, None)

    def keys(self, pattern):
        prefix = pattern.removesuffix("*")
        return [key for key in self.zsets if key.startswith(prefix)]

    def ping(self):
        return True

    def eval(self, *_args):
        return 1


class FakeRepository:
    def __init__(self):
        self.saved = []

    def save_capture(self, rows, events, industries, **kwargs):
        self.saved.append((rows, events, industries, kwargs))
        return {"snapshots": len(rows), "events": len(events), "industries": len(industries)}


def test_stage_capture_publishes_one_latest_bundle_and_restores_previous_rows():
    client = FakeRedis()
    cache = MarketIndicatorCache(client)
    captured_at = datetime(2026, 8, 4, 7, 0, tzinfo=timezone.utc)

    staged = cache.stage_capture(
        captured_at=captured_at,
        rows=[{"code": "000001", "captured_at": captured_at, "last_price": 10}],
        events=[{"code": "000001", "event_at": captured_at, "event_type": "rapid_rise"}],
        industry_rows=[],
        candidate_rows=[],
        source="eltdx.quotes",
        transmission={"episodes": [{"episode_id": "e1"}]},
    )

    latest = cache.latest_snapshots(captured_at.date())
    assert staged["pending_captures"] == 1
    assert latest["000001"]["captured_at"] == captured_at
    raw = client.get(cache._capture_key(captured_at.date().isoformat(), captured_at.isoformat()))
    assert '"episode_id":"e1"' in raw


def test_flush_acknowledges_only_after_repository_commit():
    client = FakeRedis()
    cache = MarketIndicatorCache(client)
    captured_at = datetime.now(timezone.utc) - timedelta(seconds=300)
    cache.stage_capture(
        captured_at=captured_at,
        rows=[{"code": "000001", "captured_at": captured_at}],
        events=[],
        industry_rows=[],
        candidate_rows=[],
        source="eltdx.quotes",
    )
    repo = FakeRepository()

    result = cache.flush_due(repo, min_age_seconds=180, max_captures=1)

    assert result["captures"] == 1
    assert result["snapshots"] == 1
    assert cache.pending_count(captured_at.date()) == 0
    assert len(repo.saved) == 1


def test_flush_keeps_pending_capture_when_repository_fails():
    client = FakeRedis()
    cache = MarketIndicatorCache(client)
    captured_at = datetime.now(timezone.utc) - timedelta(seconds=300)
    cache.stage_capture(
        captured_at=captured_at,
        rows=[{"code": "000001", "captured_at": captured_at}],
        events=[],
        industry_rows=[],
        candidate_rows=[],
        source="eltdx.quotes",
    )

    class FailingRepository:
        def save_capture(self, *args, **kwargs):
            raise RuntimeError("database unavailable")

    try:
        cache.flush_due(FailingRepository(), min_age_seconds=180, max_captures=1)
    except RuntimeError:
        pass

    assert cache.pending_count(captured_at.date()) == 1


def test_live_only_stage_does_not_accumulate_pending_captures():
    client = FakeRedis()
    cache = MarketIndicatorCache(client, prefix="cynexus:market-indicator:priority")
    captured_at = datetime.now(timezone.utc)
    staged = cache.stage_capture(
        captured_at=captured_at,
        rows=[{"code": "000001", "captured_at": captured_at}],
        events=[],
        industry_rows=[],
        candidate_rows=[],
        source="eltdx.quotes",
        retain_pending=False,
    )

    assert staged["pending_captures"] == 0
    assert cache.pending_count(captured_at.date()) == 0
    assert cache.latest_snapshots(captured_at.date())["000001"]["code"] == "000001"
