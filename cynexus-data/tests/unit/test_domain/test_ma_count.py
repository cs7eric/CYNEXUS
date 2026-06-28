from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.ma_count import calculate_ma_count_summary, calculate_profit_effect


def test_calculate_ma_count_summary() -> None:
    rows = [
        {"close": 12, "ma20": 10, "ma60": 11, "close_1d_ago": 11, "close_5d_ago": 10, "close_59d_ago": 15, "min_60d": 9, "close_251d_ago": 8, "max_252d": 12},
        {"close": 8, "ma20": 10, "ma60": 9, "close_1d_ago": 9, "close_5d_ago": 9, "close_59d_ago": 11, "min_60d": 8, "close_251d_ago": 10, "max_252d": 14},
    ]

    payload = calculate_ma_count_summary(rows, trade_date=date(2026, 6, 26))

    assert payload["totalEligible"] == 2
    assert payload["pctAboveMa20"] == 50.0
    assert payload["pctAboveMa60"] == 50.0
    assert payload["pctUp5d"] == 50.0
    assert payload["pctNewLow60d"] == 50.0
    assert payload["pctNewHigh252d"] == 50.0
    assert payload["breadthRaw"] == 50.0


def test_calculate_profit_effect() -> None:
    payload = calculate_profit_effect(trade_date=date(2026, 6, 26), up_5d_pct=60, new_low_60d_pct=10)

    assert payload["score"] == 72.0
