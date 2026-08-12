from __future__ import annotations

from datetime import date, timedelta

from cynexus_data.domain.market.msi_factors import (
    build_limit_snapshot_rows,
    calculate_limit_emotion,
    calculate_risk_appetite,
    calculate_style_risk,
    calculate_turnover_activity,
    calculate_volatility_sentiment,
    extract_limit_stock_snapshots,
)


def test_calculate_turnover_activity() -> None:
    rows = [
        {"trade_date": "2026-06-24", "total_amount": 100},
        {"trade_date": "2026-06-25", "total_amount": 200},
        {"trade_date": "2026-06-26", "total_amount": 300, "sh_amount": 120, "sz_amount": 180},
    ]
    payload = calculate_turnover_activity(rows, trade_date=date(2026, 6, 26), window=2)
    assert payload is not None
    assert payload["avg20dAmount"] == 150
    assert payload["ratio"] == 2


def test_calculate_volatility_sentiment() -> None:
    start = date(2026, 1, 1)
    rows = [{"trade_date": (start + timedelta(days=i)).isoformat(), "close": 100 + i + (i % 3)} for i in range(35)]
    payload = calculate_volatility_sentiment(rows, trade_date=rows[-1]["trade_date"], window=5, lookback=20)
    assert payload is not None
    assert payload["sentimentScore"] >= 0
    assert payload["sampleCount"] > 0


def test_calculate_risk_and_style() -> None:
    hs = [{"trade_date": "2026-06-24", "close": 100}, {"trade_date": "2026-06-26", "close": 110}]
    t1 = [{"trade_date": "2026-06-24", "close": 100}, {"trade_date": "2026-06-26", "close": 101}]
    t2 = [{"trade_date": "2026-06-24", "close": 100}, {"trade_date": "2026-06-26", "close": 103}]
    risk = calculate_risk_appetite(trade_date=date(2026, 6, 26), hs300_rows=hs, treasury_511010_rows=t1, treasury_511090_rows=t2, window=2)
    assert risk is not None
    assert risk["spread"]["weighted"] == 8
    style = calculate_style_risk(trade_date=date(2026, 6, 26), hs300_rows=hs, csi1000_rows=[{"trade_date": "2026-06-24", "close": 100}, {"trade_date": "2026-06-26", "close": 120}], window=2)
    assert style is not None
    assert style["spread"] == 10


def test_calculate_limit_emotion() -> None:
    today = [
        {"code": "000001", "isLimitUp": True, "isTouchedLimitUp": True, "isBrokenLimitUp": False, "changePct": 10},
        {"code": "000002", "isLimitDown": True, "isTouchedLimitUp": True, "isBrokenLimitUp": True, "changePct": -10},
    ]
    prev = [{"code": "000001", "isLimitUp": True}]
    payload = calculate_limit_emotion(trade_date=date(2026, 6, 26), today_rows=today, prev_rows=prev, prev_trade_date=date(2026, 6, 25))
    assert payload["limitUpCount"] == 1
    assert payload["limitDownCount"] == 1
    assert payload["yesterdayLimitUpAvgReturn"] == 10


def test_extract_limit_stock_snapshots() -> None:
    rows = [
        {"code": "1", "name": "平安", "isLimitUp": True, "limitUpStreak": 2, "changePct": 10.0, "limitUpPrice": 11.0, "close": 11.0},
        {"code": "000002", "isLimitDown": True, "limitDownPrice": 9.0, "close": 9.0},
        {"code": "000003", "isBrokenLimitUp": True, "previousLimitUpStreak": 3, "changePct": 5.0},
        {"code": "000004", "changePct": 1.0},
    ]

    snapshots = extract_limit_stock_snapshots(rows, trade_date=date(2026, 6, 26))

    assert [row["category"] for row in snapshots] == ["limit_up", "limit_down", "broken"]
    assert snapshots[0]["code"] == "000001"
    assert snapshots[0]["streak"] == 2
    assert snapshots[2]["streak"] == 3


def test_build_limit_snapshot_rows_uses_stock_meta_threshold() -> None:
    rows = [
        {"code": "600000", "open": 10, "high": 10.5, "low": 9.8, "close": 10.5, "preClose": 10},
        {"code": "000001", "open": 10, "high": 10.8, "low": 9.8, "close": 10.5, "preClose": 10},
    ]
    snapshots = build_limit_snapshot_rows(rows, stock_meta={"600000": {"is_st": True, "limit_up_threshold": 0.05}})
    by_code = {row["code"]: row for row in snapshots}
    assert by_code["600000"]["isLimitUp"] is False
    assert by_code["600000"]["limitThreshold"] == 0.10
    assert by_code["000001"]["isLimitUp"] is False


def test_build_limit_snapshot_rows_uses_board_limit_for_stocks() -> None:
    rows = [
        {"code": "300001", "open": 12, "high": 14.4, "low": 12, "close": 14.4, "preClose": 12},
        {"code": "688001", "open": 12, "high": 14.4, "low": 12, "close": 14.4, "preClose": 12},
        {"code": "430001", "open": 10, "high": 13, "low": 10, "close": 13, "preClose": 10},
    ]
    snapshots = build_limit_snapshot_rows(
        rows,
        stock_meta={code: {"is_st": True, "limit_up_threshold": 0.05} for code in ("300001", "688001", "430001")},
    )
    by_code = {row["code"]: row for row in snapshots}
    assert by_code["300001"]["limitThreshold"] == 0.20
    assert by_code["688001"]["limitThreshold"] == 0.20
    assert by_code["430001"]["limitThreshold"] == 0.30
    assert all(by_code[code]["isLimitUp"] for code in ("300001", "688001", "430001"))


def test_build_limit_snapshot_rows_keeps_stock_name_from_meta() -> None:
    rows = [
        {"code": "600000", "open": 10, "high": 11, "low": 10, "close": 11, "preClose": 10},
    ]

    snapshots = build_limit_snapshot_rows(
        rows,
        stock_meta={"600000": {"name": "浦发银行", "limit_up_threshold": 0.10}},
    )

    assert snapshots[0]["name"] == "浦发银行"


def test_build_limit_snapshot_rows_computes_historical_streaks() -> None:
    history = [
        {"code": "600000", "trade_date": "2026-06-23", "close": 11.0, "preClose": 10.0},
        {"code": "600000", "trade_date": "2026-06-24", "close": 12.1, "preClose": 11.0},
        {"code": "600001", "trade_date": "2026-06-24", "close": 11.0, "preClose": 10.0},
    ]
    rows = [
        {"code": "600000", "open": 12, "high": 13.31, "low": 12, "close": 13.31, "preClose": 12.1},
        {"code": "600001", "open": 11, "high": 12.1, "low": 10.5, "close": 11.5, "preClose": 11.0},
    ]
    snapshots = build_limit_snapshot_rows(rows, history_rows=history)
    by_code = {row["code"]: row for row in snapshots}
    assert by_code["600000"]["previousLimitUpStreak"] == 2
    assert by_code["600000"]["limitUpStreak"] == 3
    assert by_code["600000"]["isPromoted"] is True
    assert by_code["600001"]["previousLimitUpStreak"] == 1
    assert by_code["600001"]["isBrokenStreak"] is True


def test_build_limit_snapshot_rows_rounds_limit_price_and_keeps_streak_over_suspension_gap() -> None:
    history = [
        {"code": "600721", "trade_date": "2026-07-30", "close": 7.92, "preClose": 7.2},
        {"code": "600721", "trade_date": "2026-07-31", "close": 8.71, "preClose": 7.92},
    ]
    rows = [
        {"code": "600721", "open": 12.75, "high": 12.75, "low": 12.57, "close": 12.75, "preClose": 11.59},
    ]

    snapshots = build_limit_snapshot_rows(rows, history_rows=history)

    assert snapshots[0]["limitUpPrice"] == 12.75
    assert snapshots[0]["isLimitUp"] is True
    assert snapshots[0]["previousLimitUpStreak"] == 2
    assert snapshots[0]["limitUpStreak"] == 3
