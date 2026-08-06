from datetime import datetime, timezone

from cynexus_data.domain.market.market_indicator import (
    build_indicator_industry_snapshots,
    detect_indicator_events,
    normalize_indicator_quote,
    select_indicator_candidates,
)


def test_normalize_quote_derives_change_and_limit_status():
    row = normalize_indicator_quote(
        {"last_price": 11, "pre_close_price": 10, "change_pct": 10, "amount": 100},
        code="sh600000",
        name="浦发银行",
        market="SH",
        captured_at=datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc),
    )
    assert row["code"] == "600000"
    assert row["name"] == "浦发银行"
    assert row["limit_status"] == "limit_up_sealed"


def test_detect_events_only_on_transitions_or_fast_move():
    captured = datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc)
    previous = {"code": "600000", "limit_status": "limit_up_touch", "change_pct": 9.6}
    current = {"code": "600000", "name": "浦发银行", "market": "SH", "limit_status": "limit_up_sealed", "change_pct": 10, "last_price": 11, "captured_at": captured}
    events = detect_indicator_events(current, previous)
    assert {event["event_type"] for event in events} == {"limit_up_sealed"}


def test_detect_events_does_not_emit_rapid_move_for_first_snapshot():
    current = {"code": "600000", "change_pct": 10, "limit_status": "normal", "captured_at": datetime.now(timezone.utc)}
    assert detect_indicator_events(current, None) == []


def test_normalize_quote_interprets_metadata_threshold_as_ratio():
    row = normalize_indicator_quote(
        {"last_price": 11, "pre_close_price": 10, "change_pct": 10.0},
        code="000001",
        name="平安银行",
        market="SZ",
        captured_at=datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc),
        limit_up_threshold=0.10,
    )
    assert row["limit_status"] == "limit_up_sealed"


def test_normalize_quote_does_not_mark_normal_change_as_limit():
    row = normalize_indicator_quote(
        {"last_price": 11.11, "pre_close_price": 11.10, "change_pct": 0.09},
        code="000001",
        name="平安银行",
        market="SZ",
        captured_at=datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc),
        limit_up_threshold=0.10,
    )
    assert row["limit_status"] == "normal"


def test_normalize_quote_uses_price_and_order_book_for_touch_and_sealed():
    captured = datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc)
    touched = normalize_indicator_quote(
        {"last_price": 10.99, "pre_close_price": 10, "high_price": 11, "sell_levels": [{"price": 11, "volume": 120}]},
        code="000001", name="平安银行", market="SZ", captured_at=captured,
    )
    sealed = normalize_indicator_quote(
        {"last_price": 11, "pre_close_price": 10, "high_price": 11, "sell_levels": []},
        code="000001", name="平安银行", market="SZ", captured_at=captured,
    )
    assert touched["limit_status"] == "limit_up_touch"
    assert sealed["limit_status"] == "limit_up_sealed"


def test_normalize_quote_derives_turnover_from_float_shares_and_hands():
    row = normalize_indicator_quote(
        {"last_price": 10, "pre_close_price": 10, "total_hand": 1000},
        code="000001", name="平安银行", market="SZ", captured_at=datetime.now(timezone.utc),
        float_shares=1_000_000,
    )
    assert row["turnover_rate"] == 10.0


def test_normalize_quote_rejects_zero_price_placeholder():
    row = normalize_indicator_quote(
        {"last_price": 0, "pre_close_price": 19.09},
        code="688277",
        name="天智航",
        market="SH",
        captured_at=datetime(2026, 7, 28, 1, 42, tzinfo=timezone.utc),
    )
    assert row["last_price"] is None
    assert row["change_pct"] is None
    assert row["limit_status"] == "normal"


def test_build_industry_snapshot_tracks_first_limit_and_first_event():
    captured = datetime(2026, 7, 27, 3, 30, tzinfo=timezone.utc)
    rows = [
        {"code": "000001", "name": "平安银行", "change_pct": 10.0, "limit_status": "limit_up_sealed", "captured_at": captured},
        {"code": "000002", "name": "万科A", "change_pct": 1.2, "limit_status": "normal", "captured_at": captured},
    ]
    events = [
        {"code": "000002", "name": "万科A", "event_type": "rapid_rise", "event_at": captured, "signal_value": 3.1, "snapshot": rows[1]},
        {"code": "000001", "name": "平安银行", "event_type": "limit_up_sealed", "event_at": captured, "signal_value": 10.0, "snapshot": rows[0]},
    ]
    result = build_indicator_industry_snapshots(
        rows,
        events,
        {"000001": {"industry_code": "0:银行", "industry_name": "银行"}, "000002": {"industry_code": "0:银行", "industry_name": "银行"}},
    )
    assert len(result) == 1
    assert result[0]["industry_name"] == "银行"
    assert result[0]["limit_up_count"] == 1
    assert result[0]["rapid_rise_count"] == 1
    assert result[0]["first_limit_up_code"] == "000001"
    assert result[0]["first_event_code"] == "000002"


def test_select_indicator_candidates_keeps_hot_signals_and_rotates_cold_codes():
    captured = datetime(2026, 7, 28, 2, 0, tzinfo=timezone.utc)
    metadata = {
        "000001": {"name": "A"},
        "000002": {"name": "B"},
        "000003": {"name": "C"},
        "000004": {"name": "D"},
    }
    selected = select_indicator_candidates(
        metadata,
        {"000002": {"change_pct": 9.8, "amount": 500_000_000, "limit_status": "limit_up_touch"}},
        captured_at=captured,
        candidate_limit=2,
        hot_limit=1,
        rotation_limit=1,
        rotation_buckets=2,
        rotation_index=0,
    )

    assert list(selected) == ["000002", "000004"]
    assert selected["000002"]["candidate_tier"] == "hot"
    assert selected["000004"]["candidate_reason"] == "rotation"
