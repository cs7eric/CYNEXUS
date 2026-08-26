import base64
import gzip
import json
from datetime import date, datetime, timezone
from types import SimpleNamespace

import pytest

import cynexus_data.pipelines.market.build_trade_review_market_state as market_state_pipeline
from cynexus_data.domain.market.trade_review_market_state import (
    build_daily_projection,
    build_limit_intervals,
    build_minute_projections,
    build_security_daily_facts,
    classify_phase,
    normalize_indicator_events,
)
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.build_trade_review_market_state import _publish_cache
from cynexus_data.storage.postgres.trade_review_market_state import TradeReviewMarketStateRepository


def _contexts():
    return {
        "000001": {
            "code": "000001", "name": "甲", "security_type": "STOCK", "pre_close": 10,
            "limit_up_price": 11, "limit_down_price": 9, "previous_streak": 1,
            "previous_limit_status": "limit_up",
            "memberships": [{"source": "ths", "code": "I1", "name": "行业甲"}, {"source": "union", "code": "I1", "name": "行业甲"}],
        },
        "000002": {
            "code": "000002", "name": "乙", "security_type": "STOCK", "pre_close": 10,
            "limit_up_price": 11, "limit_down_price": 9, "previous_streak": 0,
            "memberships": [{"source": "ths", "code": "I1", "name": "行业甲"}, {"source": "union", "code": "I1", "name": "行业甲"}],
        },
        "510300": {
            "code": "510300", "name": "ETF", "security_type": "ETF", "pre_close": 4,
            "limit_up_price": None, "limit_down_price": None, "previous_streak": 0, "memberships": [],
        },
    }


def test_minute_projection_keeps_touched_state_and_excludes_etf_emotion():
    first = datetime(2026, 8, 10, 9, 31)
    second = datetime(2026, 8, 10, 9, 32)
    projections = build_minute_projections([
        {"symbol": "000001", "trade_time": first, "high": 11, "close": 11, "low": 10, "volume": 1, "amount": 1},
        {"symbol": "000002", "trade_time": first, "high": 10.2, "close": 10.2, "low": 10, "volume": 1, "amount": 1},
        {"symbol": "510300", "trade_time": first, "high": 4.1, "close": 4.1, "low": 4, "volume": 1, "amount": 1},
        {"symbol": "000001", "trade_time": second, "high": 11, "close": 10.7, "low": 10.6, "volume": 1, "amount": 1},
    ], _contexts())

    assert len(projections) == 2
    assert projections[0].market["sealed_limit_up_count"] == 1
    assert projections[0].market["max_height"] == 2
    assert projections[0].market["promoted_count"] == 1
    assert projections[0].market["promotion_rate"] == 100.0
    assert projections[0].market["median_change_pct"] == pytest.approx(6.0)
    assert projections[1].market["touched_limit_up_count"] == 1
    assert projections[1].market["broken_limit_up_count"] == 1
    assert projections[1].market["advancing_count"] == 2
    sealed_stock = projections[0].pools[0]["stocks"][0]
    broken_stock = next(pool for pool in projections[1].pools if pool["pool_type"] == "broken")["stocks"][0]
    assert sealed_stock["firstSealAt"] == first
    assert broken_stock["firstSealAt"] == first
    assert broken_stock["openCount"] == 1
    industry = next(item for item in projections[0].industries if item["membership"] == "ths")
    assert industry["stock_count"] == 2
    industry_ladder = next(
        item for item in projections[0].pools
        if item["membership"] == "union" and item["industry_code"] == "I1" and item["pool_type"] == "ladder"
    )
    assert [stock["code"] for stock in industry_ladder["stocks"]] == ["000001"]


def test_minute_projection_collapses_aliases_of_the_same_industry_code():
    contexts = _contexts()
    contexts["000001"]["memberships"].append({"source": "tdx", "code": "880001", "name": "旧名称"})
    contexts["000002"]["memberships"].append({"source": "tdx", "code": "880001", "name": "新名称"})
    projections = build_minute_projections([
        {"symbol": "000001", "trade_time": datetime(2026, 8, 10, 9, 31), "high": 10.1, "close": 10.1},
        {"symbol": "000002", "trade_time": datetime(2026, 8, 10, 9, 31), "high": 10.2, "close": 10.2},
    ], contexts)

    matches = [item for item in projections[0].industries if item["membership"] == "tdx" and item["industry_code"] == "880001"]
    assert len(matches) == 1
    assert matches[0]["stock_count"] == 2


def test_raw_loaders_keep_stock_market_when_index_and_stock_share_a_code():
    """SH000001 must never become the SZ000001 stock in replay aggregates."""
    class ClickHouse:
        def query_rows(self, sql, _params):
            if "FROM ch_dwd_kline_1m" in sql:
                return [
                    ("SH", "000001", datetime(2026, 8, 7, 9, 31), 3898.0, 3900.0, 3890.0, 3895.0, 1, 1),
                    ("SZ", "000001", datetime(2026, 8, 7, 9, 31), 11.15, 11.2, 11.1, 11.17, 1, 1),
                ]
            return [("SH", "000001", 3900.35), ("SZ", "000001", 11.27)]

    context = SimpleNamespace(hooks=SimpleNamespace(clickhouse=SimpleNamespace(get_client=lambda: ClickHouse())))
    metadata = {"000001": {"market": "SZ", "security_type": "STOCK"}}

    bars = market_state_pipeline._load_minute_bars(context, date(2026, 8, 7), metadata)
    previous_closes = market_state_pipeline._load_previous_closes(context, date(2026, 8, 7), metadata)

    assert len(bars) == 1
    assert bars[0]["market"] == "SZ"
    assert bars[0]["open"] == 11.15
    assert bars[0]["close"] == 11.17
    assert previous_closes == {"000001": 11.27}


def test_indicator_events_are_normalized_and_intervals_are_cursor_safe():
    at = datetime(2026, 8, 10, 9, 45, 11)
    opened = datetime(2026, 8, 10, 10, 2, 0)
    resealed = datetime(2026, 8, 10, 10, 7, 2)
    events = normalize_indicator_events([
        {"code": "000001", "event_type": "limit_up_sealed", "event_at": at, "last_price": 11},
        {"code": "000001", "event_type": "limit_up_opened", "event_at": opened, "last_price": 10.8},
        {"code": "000001", "event_type": "limit_up_sealed", "event_at": resealed, "last_price": 11},
    ], _contexts())
    intervals = build_limit_intervals(events)

    assert [item["event_type"] for item in events] == ["LIMIT_UP_SEAL", "LIMIT_UP_OPEN", "LIMIT_UP_SEAL"]
    assert intervals[0]["state"] == "SEALED"
    assert intervals[0]["ended_at"] == opened
    assert intervals[-1]["state"] == "SEALED"
    assert intervals[-1]["ended_at"] is None


def test_daily_projection_keeps_process_and_yesterday_feedback_pool_details():
    first = datetime(2026, 8, 10, 9, 31)
    second = datetime(2026, 8, 10, 9, 32)
    third = datetime(2026, 8, 10, 9, 33)
    projections = build_minute_projections([
        {"symbol": "000001", "trade_time": first, "high": 11, "close": 11},
        {"symbol": "000001", "trade_time": second, "high": 11, "close": 10.8},
        {"symbol": "000001", "trade_time": third, "high": 11, "close": 11},
    ], _contexts())
    _, pools = build_daily_projection(projections, _contexts(), [
        {"code": "000001", "event_type": "LIMIT_UP_SEAL", "event_at": first},
        {"code": "000001", "event_type": "LIMIT_UP_OPEN", "event_at": second},
        {"code": "000001", "event_type": "LIMIT_UP_SEAL", "event_at": third},
    ])
    resealed = next(pool for pool in pools if pool["pool_type"] == "reseal")
    feedback = next(pool for pool in pools if pool["pool_type"] == "yesterday_limit_up_feedback")
    industry_core = next(pool for pool in pools if pool["pool_type"] == "industry_core")
    assert resealed["stocks"][0]["code"] == "000001"
    assert resealed["stocks"][0]["resealCount"] == 1
    assert resealed["stocks"][0]["openCount"] == 1
    assert feedback["stocks"][0]["firstSealAt"].startswith("2026-08-10T09:31")
    assert industry_core["membership"] == "ths"
    assert industry_core["stocks"][0]["code"] == "000001"


def test_daily_projection_materializes_scoped_industry_pools_and_raw_daily_facts():
    first = datetime(2026, 8, 10, 9, 31)
    second = datetime(2026, 8, 10, 9, 32)
    bars = [
        {"symbol": "000001", "trade_time": first, "open": 10.2, "high": 11, "low": 11, "close": 11, "volume": 2, "amount": 22},
        {"symbol": "000001", "trade_time": second, "high": 11, "low": 10.8, "close": 10.8, "volume": 3, "amount": 32.4},
        {"symbol": "000002", "trade_time": second, "high": 10.3, "low": 10.1, "close": 10.2, "volume": 1, "amount": 10.2},
    ]
    projections = build_minute_projections(bars, _contexts())
    _, pools = build_daily_projection(projections, _contexts())
    scoped_broken = next(pool for pool in pools if pool["pool_type"] == "broken" and pool["membership"] == "ths" and pool["industry_code"] == "I1")
    assert scoped_broken["stocks"][0]["code"] == "000001"

    facts = {fact["code"]: fact for fact in build_security_daily_facts(bars, _contexts())}
    assert facts["000001"]["touch_limit_up"] is True
    assert facts["000001"]["open"] == 10.2
    assert facts["000001"]["final_limit_up"] is False
    assert facts["000001"]["open_count"] == 1
    assert facts["000001"]["volume"] == 5
    assert facts["000002"]["change_pct"] == pytest.approx(2.0)


def test_daily_projection_uses_persisted_limit_rows_for_post_close_counts_and_ladder():
    projections = market_state_pipeline.build_minute_projections([
        {"symbol": "000001", "trade_time": datetime(2026, 8, 11, 15, 0), "high": 10.2, "close": 10.2},
        {"symbol": "000002", "trade_time": datetime(2026, 8, 11, 15, 0), "high": 10.2, "close": 10.2},
    ], _contexts())

    market, pools = build_daily_projection(
        projections,
        _contexts(),
        current_limit_rows={
            "000001": {"code": "000001", "name": "甲", "category": "limit_up", "streak": 6, "change_pct": 10.0},
            "000002": {"code": "000002", "name": "乙", "category": "limit_up", "streak": 1, "change_pct": 10.0},
            "000003": {"code": "000003", "name": "丙", "category": "broken", "streak": 0, "change_pct": 8.0},
        },
    )

    limit_up = next(pool for pool in pools if pool["pool_type"] == "limit_up" and pool["membership"] == "union" and not pool["industry_code"])
    ladder = next(pool for pool in pools if pool["pool_type"] == "ladder" and pool["membership"] == "union" and not pool["industry_code"])
    assert market["sealed_limit_up_count"] == 2
    assert market["touched_limit_up_count"] == 3
    assert market["broken_limit_up_count"] == 1
    assert market["max_height"] == 6
    assert market["ladder"] == [{"height": 6, "count": 1}, {"height": 1, "count": 1}]
    assert [stock["code"] for stock in limit_up["stocks"]] == ["000001", "000002"]
    assert [stock["height"] for stock in ladder["stocks"]] == [6, 1]


def test_daily_facts_accept_postgres_aware_indicator_time_with_clickhouse_naive_bar_time():
    bar_at = datetime(2026, 8, 10, 9, 31)
    event_at = datetime(2026, 8, 10, 1, 30, 30, tzinfo=timezone.utc)
    facts = build_security_daily_facts(
        [{"symbol": "000001", "trade_time": bar_at, "high": 11, "low": 11, "close": 11}],
        _contexts(),
        [{"code": "000001", "event_type": "LIMIT_UP_SEAL", "event_at": event_at}],
    )
    assert facts[0]["first_limit_up_at"] == event_at


def test_phase_exposes_window_coverage_and_a_stable_policy_version():
    phase = classify_phase([{
        "seal_rate": 70, "break_rate": 10, "sealed_limit_up_count": 50,
        "sealed_limit_down_count": 1, "max_height": 4,
    }], window_days=60)

    assert phase["policyVersion"] == "daily-v2"
    assert phase["windowDays"] == 60
    assert phase["dataCoverage"] == pytest.approx(0.0167)
    assert phase["factor"]["dataCoverage"] == pytest.approx(0.0167)


def test_range_backfill_uses_only_raw_trading_dates_and_processes_them_in_order(monkeypatch):
    class ClickHouse:
        def query_rows(self, _sql, _params):
            return [(date(2026, 8, 7),), (date(2026, 8, 10),)]

    context = SimpleNamespace(hooks=SimpleNamespace(clickhouse=SimpleNamespace(get_client=lambda: ClickHouse())))
    calls = []

    def materialize(_context, _request, trade_date):
        calls.append(trade_date)
        return {"rows_read": 10, "rows_written": 20, "trade_date": trade_date.isoformat()}

    monkeypatch.setattr(market_state_pipeline, "_run_with_failure_manifest", materialize)
    result = market_state_pipeline.run(context, JobRequest(
        job_key="build_trade_review_market_state",
        params={"start_date": "2026-08-07", "end_date": "2026-08-10"},
    ))

    assert calls == [date(2026, 8, 7), date(2026, 8, 10)]
    assert result["rows_read"] == 20
    assert result["rows_written"] == 40


def test_market_state_cache_bundle_keeps_industry_phases_for_redis_first_reads():
    class Redis:
        def __init__(self):
            self.writes = []

        def setex(self, key, ttl, value):
            self.writes.append((key, ttl, value))

    redis = Redis()
    context = SimpleNamespace(hooks=SimpleNamespace(redis=SimpleNamespace(get_binary_client=lambda: redis)))
    projection = SimpleNamespace(
        minute_at=datetime(2026, 8, 10, 9, 31),
        market={"minute_at": datetime(2026, 8, 10, 9, 31)}, industries=[], pools=[],
    )

    published = _publish_cache(
        context, date(2026, 8, 10), {"max_height": 2}, [], {"phase": "启动"}, [projection], [], [], [],
        [{"membership": "union", "industry_code": "I1", "phase": "加速"}],
        {"status": "READY"}, JobRequest(job_key="build_trade_review_market_state"),
    )

    assert published is True
    key, ttl, raw = redis.writes[-1]
    assert key.endswith(":day:2026-08-10")
    assert ttl == 604800
    decoded = gzip.decompress(base64.b64decode(raw.split(b"gzip:", 1)[1]))
    assert json.loads(decoded)["industryPhases"][0]["phase"] == "加速"
    assert any(key.endswith(":minute:0931") for key, _, _ in redis.writes)
    assert any(key.endswith(":events") for key, _, _ in redis.writes)


def test_failed_materialization_writes_a_best_effort_manifest(monkeypatch):
    calls = []

    class Repository:
        def update_manifest(self, trade_date, **values):
            calls.append((trade_date, values))

    def fail(*_args, **_kwargs):
        raise RuntimeError("raw minute unavailable")

    context = SimpleNamespace(hooks=SimpleNamespace(postgres=SimpleNamespace(get_client=lambda: object())))
    monkeypatch.setattr(market_state_pipeline, "_run", fail)
    monkeypatch.setattr(market_state_pipeline, "TradeReviewMarketStateRepository", lambda _client: Repository())

    with pytest.raises(RuntimeError, match="raw minute unavailable"):
        market_state_pipeline.run(context, JobRequest(
            job_key="build_trade_review_market_state", params={"trade_date": "2026-08-10"},
        ))

    assert calls[0][0] == date(2026, 8, 10)
    assert calls[0][1]["status"] == "FAILED"


def test_manifest_serializes_quality_before_postgres_binding():
    recorded = []

    class Session:
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return False

        def execute(self, _statement, params):
            recorded.append(params)

    client = SimpleNamespace(schema="cynexus_appl_market", session=lambda: Session())
    TradeReviewMarketStateRepository(client).update_manifest(
        date(2026, 8, 7), status="READY", minute_bar_count=241, minute_symbol_count=1,
        context_count=1, indicator_event_count=0, market_minute_count=241,
        industry_minute_count=1, daily_pool_count=1, cache_published=True,
        quality={"coverageComplete": True}, error_message=None,
    )

    assert json.loads(recorded[0]["quality"]) == {"coverageComplete": True}
