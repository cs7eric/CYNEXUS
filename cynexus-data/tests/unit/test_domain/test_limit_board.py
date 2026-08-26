from datetime import datetime, timedelta, timezone

from cynexus_data.domain.market.limit_board import build_limit_board_sample, build_limit_board_snapshot
from cynexus_data.domain.market.limit_board_session import aggregate_limit_board_session, current_trade_session


def _row(status="limit_up_sealed"):
    return {
        "code": "000001",
        "name": "平安银行",
        "market": "SZ",
        "last_price": 11.0,
        "prev_close": 10.0,
        "change_pct": 10.0,
        "limit_up_price": 11.0,
        "limit_down_price": 9.0,
        "limit_status": status,
        "captured_at": datetime(2026, 8, 11, 2, 0, tzinfo=timezone.utc),
        "raw_payload": {
            "time_raw": 1400,
            "buy_levels": [
                {"price": 11.0, "volume": 1000},
                {"price": 10.99, "volume": 200},
            ],
            "sell_levels": [{"price": 11.0, "volume": 0}],
        },
    }


def test_limit_up_sealed_amount_uses_buy_queue_in_lots():
    snapshot = build_limit_board_snapshot(_row())

    assert snapshot is not None
    assert snapshot["direction"] == "up"
    assert snapshot["board_status"] == "sealed"
    assert snapshot["queue_side"] == "buy"
    assert snapshot["queue_volume"] == 1000
    assert snapshot["sealed_amount"] == 1_100_000
    assert snapshot["bid_price_1"] == 11.0
    assert snapshot["ask_volume_1"] == 0


def test_sealed_to_touch_is_recorded_as_opened():
    snapshot = build_limit_board_snapshot(
        _row("limit_up_touch"),
        previous={"direction": "up", "board_status": "sealed"},
    )

    assert snapshot is not None
    assert snapshot["board_status"] == "opened"
    assert snapshot["is_sealed"] is False
    assert snapshot["sealed_amount"] is None


def test_normal_quote_is_ignored_without_previous_sealed_board():
    assert build_limit_board_snapshot({"limit_status": "normal"}) is None


def test_transient_sample_keeps_normal_quote_after_board_was_seen():
    sample = build_limit_board_sample(
        {
            **_row("normal"),
            "last_price": 10.5,
            "captured_at": datetime(2026, 8, 11, 2, 0, 5, tzinfo=timezone.utc),
        },
        prior_direction="up",
    )

    assert sample is not None
    assert sample["direction"] == "up"
    assert sample["board_status"] == "opened"
    assert sample["is_on_board"] is False
    assert sample["sealed_amount"] is None


def test_limit_board_sample_keeps_quote_turnover():
    sample = build_limit_board_sample({**_row(), "amount": 2_847_336_32})

    assert sample is not None
    assert sample["turnover_amount"] == 2_847_336_32


def test_high_only_limit_status_is_not_on_board():
    sample = build_limit_board_sample({
        **_row("limit_up_touch"),
        "last_price": 95.07,
        "limit_up_price": 99.23,
        "limit_down_price": 81.19,
    })

    assert sample is not None
    assert sample["direction"] == "up"
    assert sample["is_on_board"] is False
    assert sample["is_sealed"] is False
    assert sample["board_status"] == "opened"
    assert sample["sealed_amount"] is None


def test_last_price_at_limit_down_wins_over_high_limit_up_status():
    sample = build_limit_board_sample({
        **_row("limit_up_touch"),
        "last_price": 11.64,
        "limit_up_price": 14.22,
        "limit_down_price": 11.64,
    })

    assert sample is not None
    assert sample["direction"] == "down"
    assert sample["is_on_board"] is True
    assert sample["board_status"] == "touch"


def test_limit_board_session_tracks_open_and_reseal_trend():
    base = datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc)
    rows = [
        {"code": "000001", "direction": "up", "board_status": "sealed", "is_on_board": True, "is_sealed": True, "sealed_amount": 100, "turnover_amount": 800, "captured_at": base},
        {"code": "000001", "direction": "up", "board_status": "opened", "is_on_board": False, "is_sealed": False, "turnover_amount": 900, "captured_at": base.replace(second=5)},
        {"code": "000001", "direction": "up", "board_status": "sealed", "is_on_board": True, "is_sealed": True, "sealed_amount": 130, "turnover_amount": 1000, "captured_at": base.replace(second=10)},
    ]

    summary = aggregate_limit_board_session(
        rows,
        trade_date=base.date(),
        trade_session="morning",
        sample_interval_seconds=5,
    )[0]

    assert summary["open_count"] == 1
    assert summary["reseal_count"] == 1
    assert summary["closing_status"] == "sealed"
    assert summary["trend_direction"] == "increasing"
    assert summary["sealed_amount_change"] == 30
    assert summary["sealed_duration_seconds"] == 10
    assert summary["gap_count"] == 0
    assert summary["last_turnover_amount"] == 1000
    assert summary["last_sealed_turnover_ratio"] == 0.13
    assert summary["max_sealed_turnover_ratio"] == 0.13


def test_touch_after_sealed_board_counts_as_open_without_leaving_limit_price():
    base = datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc)
    summary = aggregate_limit_board_session(
        [
            {"code": "000001", "direction": "up", "board_status": "sealed", "is_on_board": True, "is_sealed": True, "sealed_amount": 100, "captured_at": base},
            {"code": "000001", "direction": "up", "board_status": "touch", "is_on_board": True, "is_sealed": False, "captured_at": base.replace(second=5)},
        ],
        trade_date=base.date(),
        trade_session="morning",
        sample_interval_seconds=5,
    )[0]

    assert summary["open_count"] == 1
    assert summary["reseal_count"] == 0
    assert summary["closing_status"] == "touch"


def test_limit_board_session_skips_stocks_that_never_reached_last_price_limit():
    base = datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc)
    summaries = aggregate_limit_board_session(
        [
            {
                "code": "002980",
                "direction": "up",
                "board_status": "opened",
                "is_on_board": False,
                "is_sealed": False,
                "captured_at": base,
            },
            {
                "code": "002980",
                "direction": "up",
                "board_status": "opened",
                "is_on_board": False,
                "is_sealed": False,
                "captured_at": base.replace(second=5),
            },
        ],
        trade_date=base.date(),
        trade_session="morning",
        sample_interval_seconds=5,
    )

    assert summaries == []


def test_trade_session_boundaries_are_shanghai_time():
    assert current_trade_session(datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc)) == "morning"
    assert current_trade_session(datetime(2026, 8, 11, 5, 30, tzinfo=timezone.utc)) == "afternoon"
    assert current_trade_session(datetime(2026, 8, 11, 7, 0, tzinfo=timezone.utc)) is None

def test_trend_series_stays_within_postgres_limit():
    base = datetime(2026, 8, 14, 5, 0, tzinfo=timezone.utc)
    rows = [
        {
            "code": "002322",
            "direction": "up",
            "board_status": "sealed",
            "is_on_board": True,
            "is_sealed": True,
            "sealed_amount": 1000 + index,
            "captured_at": base + timedelta(seconds=index * 5),
        }
        for index in range(240)
    ]

    summary = aggregate_limit_board_session(
        rows,
        trade_date=base.date(),
        trade_session="afternoon",
        sample_interval_seconds=5,
    )[0]

    assert len(summary["trend_time_offsets"]) == len(summary["trend_sealed_amounts"])
    assert len(summary["trend_time_offsets"]) <= 120
    assert summary["trend_time_offsets"][0] == 0
    assert summary["trend_time_offsets"][-1] == 5 * 239
    assert summary["trend_sealed_amounts"][0] == 1000
    assert summary["trend_sealed_amounts"][-1] == 1000 + 239
