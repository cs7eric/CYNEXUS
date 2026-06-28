from __future__ import annotations

from datetime import date, datetime, time, timedelta
from typing import Iterable


def is_weekday(d: date) -> bool:
    return d.weekday() < 5


HOLIDAYS: dict[date, str] = {
    date(2024, 1, 1): "元旦",
    date(2024, 2, 9): "春节",
    date(2024, 2, 10): "春节",
    date(2024, 2, 11): "春节",
    date(2024, 2, 12): "春节",
    date(2024, 2, 13): "春节",
    date(2024, 2, 14): "春节",
    date(2024, 2, 15): "春节",
    date(2024, 2, 16): "春节",
    date(2024, 2, 17): "春节调休",
    date(2024, 2, 18): "春节调休",
    date(2024, 4, 4): "清明",
    date(2024, 4, 5): "清明",
    date(2024, 4, 6): "清明",
    date(2024, 4, 28): "劳动节调休",
    date(2024, 5, 1): "劳动节",
    date(2024, 5, 2): "劳动节",
    date(2024, 5, 3): "劳动节",
    date(2024, 5, 5): "劳动节调休",
    date(2024, 6, 10): "端午",
    date(2024, 9, 15): "中秋",
    date(2024, 9, 16): "中秋",
    date(2024, 9, 17): "中秋",
    date(2024, 9, 29): "国庆调休",
    date(2024, 10, 1): "国庆",
    date(2024, 10, 2): "国庆",
    date(2024, 10, 3): "国庆",
    date(2024, 10, 4): "国庆",
    date(2024, 10, 5): "国庆",
    date(2024, 10, 6): "国庆",
    date(2024, 10, 7): "国庆",
    date(2024, 10, 12): "国庆调休",
    date(2025, 1, 1): "元旦",
    date(2025, 1, 26): "春节调休",
    date(2025, 1, 28): "春节",
    date(2025, 1, 29): "春节",
    date(2025, 1, 30): "春节",
    date(2025, 1, 31): "春节",
    date(2025, 2, 3): "春节",
    date(2025, 2, 4): "春节",
    date(2025, 2, 8): "春节调休",
    date(2025, 4, 4): "清明",
    date(2025, 4, 5): "清明",
    date(2025, 4, 6): "清明",
    date(2025, 4, 27): "劳动节调休",
    date(2025, 5, 1): "劳动节",
    date(2025, 5, 2): "劳动节",
    date(2025, 5, 5): "劳动节",
    date(2025, 5, 31): "端午",
    date(2025, 6, 2): "端午",
    date(2025, 9, 28): "国庆调休",
    date(2025, 10, 1): "国庆",
    date(2025, 10, 2): "国庆",
    date(2025, 10, 3): "国庆",
    date(2025, 10, 6): "国庆",
    date(2025, 10, 7): "国庆",
    date(2025, 10, 8): "国庆",
    date(2025, 10, 11): "国庆调休",
    date(2026, 1, 1): "元旦",
    date(2026, 1, 2): "元旦",
    date(2026, 1, 4): "元旦调休",
    date(2026, 2, 14): "春节调休",
    date(2026, 2, 16): "春节",
    date(2026, 2, 17): "春节",
    date(2026, 2, 18): "春节",
    date(2026, 2, 19): "春节",
    date(2026, 2, 20): "春节",
    date(2026, 2, 23): "春节",
    date(2026, 2, 24): "春节",
    date(2026, 2, 28): "春节调休",
    date(2026, 4, 6): "清明",
    date(2026, 5, 1): "劳动节",
    date(2026, 5, 4): "劳动节",
    date(2026, 5, 5): "劳动节",
    date(2026, 5, 9): "劳动节调休",
    date(2026, 6, 19): "端午",
    date(2026, 9, 25): "中秋",
    date(2026, 9, 27): "中秋调休",
    date(2026, 10, 1): "国庆",
    date(2026, 10, 2): "国庆",
    date(2026, 10, 5): "国庆",
    date(2026, 10, 6): "国庆",
    date(2026, 10, 7): "国庆",
    date(2026, 10, 10): "国庆调休",
    date(2027, 1, 1): "元旦",
}

WORKDAYS_OVERTIME: set[date] = set()


def is_trading_day(d: date | None = None, *, extra_holidays: Iterable[date] | None = None, extra_workdays: Iterable[date] | None = None) -> bool:
    target = d or date.today()
    workdays = set(extra_workdays or ())
    holidays = set(extra_holidays or ())
    if target in WORKDAYS_OVERTIME or target in workdays:
        return True
    if target in HOLIDAYS or target in holidays:
        return False
    return is_weekday(target)


def previous_trading_day(d: date | None = None, *, extra_holidays: Iterable[date] | None = None, extra_workdays: Iterable[date] | None = None) -> date:
    return _walk_trading_day(d or date.today(), -1, extra_holidays=extra_holidays, extra_workdays=extra_workdays)


def next_trading_day(d: date | None = None, *, extra_holidays: Iterable[date] | None = None, extra_workdays: Iterable[date] | None = None) -> date:
    return _walk_trading_day(d or date.today(), 1, extra_holidays=extra_holidays, extra_workdays=extra_workdays)


def generate_trade_calendar(start: date, end: date, *, extra_holidays: Iterable[date] | None = None, extra_workdays: Iterable[date] | None = None) -> list[dict[str, object]]:
    if start > end:
        raise ValueError("start must be <= end")
    rows: list[dict[str, object]] = []
    current = start
    holiday_set = set(extra_holidays or ())
    workday_set = set(extra_workdays or ())
    while current <= end:
        reason = _reason(current, holiday_set=holiday_set, workday_set=workday_set)
        rows.append({"trade_date": current, "is_trading_day": is_trading_day(current, extra_holidays=holiday_set, extra_workdays=workday_set), "reason": reason})
        current += timedelta(days=1)
    return rows


def is_trade_time(value: datetime | None = None) -> bool:
    moment = value or datetime.now()
    if not is_trading_day(moment.date()):
        return False
    current = moment.time()
    return time(9, 30) <= current <= time(11, 30) or time(13, 0) <= current <= time(15, 0)


def _walk_trading_day(start: date, step: int, *, extra_holidays: Iterable[date] | None = None, extra_workdays: Iterable[date] | None = None) -> date:
    current = start
    while True:
        current += timedelta(days=step)
        if is_trading_day(current, extra_holidays=extra_holidays, extra_workdays=extra_workdays):
            return current


def _reason(d: date, *, holiday_set: set[date], workday_set: set[date]) -> str:
    if d in workday_set or d in WORKDAYS_OVERTIME:
        return "extra_workday"
    if d in holiday_set:
        return "extra_holiday"
    if d in HOLIDAYS:
        return HOLIDAYS[d]
    if not is_weekday(d):
        return "weekend"
    return "weekday"
