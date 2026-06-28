from __future__ import annotations

from datetime import date, datetime

from cynexus_data.core.calendar import generate_trade_calendar, is_trade_time, is_trading_day, next_trading_day, previous_trading_day


def test_a_share_calendar_rules() -> None:
    assert not is_trading_day(date(2026, 1, 1))
    assert not is_trading_day(date(2026, 1, 3))
    assert is_trading_day(date(2026, 1, 5))
    assert previous_trading_day(date(2026, 1, 5)) == date(2025, 12, 31)
    assert next_trading_day(date(2026, 1, 1)) == date(2026, 1, 5)


def test_trade_calendar_generation_with_overrides() -> None:
    rows = generate_trade_calendar(date(2026, 1, 1), date(2026, 1, 3), extra_workdays=[date(2026, 1, 3)])
    assert rows[0]["reason"] == "元旦"
    assert rows[2]["is_trading_day"] is True
    assert rows[2]["reason"] == "extra_workday"


def test_trade_time() -> None:
    assert is_trade_time(datetime(2026, 1, 5, 10, 0))
    assert not is_trade_time(datetime(2026, 1, 5, 12, 0))
