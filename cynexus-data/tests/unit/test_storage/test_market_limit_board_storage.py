from datetime import datetime, timezone

from cynexus_data.storage.postgres.market_limit_board import _snapshot_params
from cynexus_data.storage.postgres.market_limit_board_session import _summary_params
from cynexus_data.storage.redis.market_limit_board_session import MarketLimitBoardSessionCache


def test_limit_board_snapshot_id_is_stable_and_payload_is_json():
    captured_at = datetime(2026, 8, 11, 2, 0, tzinfo=timezone.utc)
    row = {"code": "000001", "captured_at": captured_at, "payload": {"limit_status": "limit_up_sealed"}}

    first = _snapshot_params(row, source="eltdx.quotes")
    second = _snapshot_params(row, source="eltdx.quotes")

    assert first["id"] == second["id"]
    assert first["trade_date"].isoformat() == "2026-08-11"
    assert first["payload"] == '{"limit_status": "limit_up_sealed"}'


def test_limit_board_session_summary_id_is_stable_and_has_no_raw_payload():
    captured_at = datetime(2026, 8, 11, 2, 0, tzinfo=timezone.utc)
    row = {
        "trade_date": captured_at.date(),
        "trade_session": "morning",
        "code": "000001",
        "direction": "up",
        "trend_time_offsets": [0, 3],
        "trend_sealed_amounts": [100, 120],
        "trend_features": {"trend_series_format": "offset_seconds_amount_yuan_v1"},
    }

    first = _summary_params(row, source="eltdx.quotes.best_quote")
    second = _summary_params(row, source="eltdx.quotes.best_quote")

    assert first["id"] == second["id"]
    assert first["source"] == "eltdx.quotes.best_quote"
    assert first["trend_time_offsets"] == [0, 3]
    assert first["trend_sealed_amounts"] == [100, 120]
    assert first["trend_features"] == '{"trend_series_format": "offset_seconds_amount_yuan_v1"}'
    assert "payload" not in first


def test_whole_market_discovery_publishes_tracked_codes():
    class _Pipeline:
        def __init__(self, client):
            self.client = client
            self.operations = []

        def sadd(self, *args, **kwargs): self.operations.append((self.client.sadd, args, kwargs)); return self
        def hset(self, *args, **kwargs): self.operations.append((self.client.hset, args, kwargs)); return self
        def expire(self, *args, **kwargs): return self
        def execute(self): return [method(*args, **kwargs) for method, args, kwargs in self.operations]

    class _Redis:
        def __init__(self): self.sets = {}; self.hashes = {}
        def pipeline(self, transaction=True): return _Pipeline(self)
        def sadd(self, key, *values): self.sets.setdefault(key, set()).update(values)
        def hset(self, key, mapping): self.hashes.setdefault(key, {}).update(mapping)
        def expire(self, *_args): return True
        def hgetall(self, key): return self.hashes.get(key, {})
        def smembers(self, key): return self.sets.get(key, set())

    cache = MarketLimitBoardSessionCache(_Redis())
    published = cache.publish_discoveries(
        trade_date="2026-08-13", trade_session="morning",
        discovered={"000001": "up", "000002": "down"},
        discovered_at=datetime(2026, 8, 13, 1, 30, tzinfo=timezone.utc),
        source="market_indicator_refresh",
    )

    assert published == 2
    assert cache.seen_directions(trade_date="2026-08-13", trade_session="morning") == {"000001": "up", "000002": "down"}
