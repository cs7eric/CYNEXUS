from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from typing import Iterable, Mapping, Sequence


SESSION_SLOTS: tuple[time, ...] = tuple(
    [datetime.combine(date.min, time(9, 30)) + timedelta(minutes=index) for index in range(120)]
    + [datetime.combine(date.min, time(13, 0)) + timedelta(minutes=index) for index in range(120)]
)
SESSION_SLOTS = tuple(value.time() for value in SESSION_SLOTS)


@dataclass(frozen=True)
class TimeshareTarget:
    trade_date: date
    target_type: str
    market: str
    symbol: str
    name: str | None = None
    priority: int = 100


def canonical_slots(trade_date: date) -> dict[datetime, int]:
    return {datetime.combine(trade_date, label): index for index, label in enumerate(SESSION_SLOTS)}


def normalize_timeshare_rows(
    rows: Iterable[Mapping[str, object]],
    *,
    trade_date: date,
    symbol: str,
    market: str,
    source: str,
) -> list[dict[str, object]]:
    slots = canonical_slots(trade_date)
    normalized: list[dict[str, object]] = []
    for row in rows:
        timestamp = _datetime(row.get("trade_time") or row.get("datetime") or row.get("date"))
        if timestamp is None:
            continue
        timestamp = timestamp.replace(second=0, microsecond=0)
        slot = slots.get(datetime.combine(trade_date, timestamp.time()))
        if slot is None:
            continue
        price = _number(row.get("price") or row.get("close"))
        if price is None or price <= 0:
            continue
        volume = _number(row.get("volume") or row.get("vol"), default=0.0)
        normalized.append(
            {
                "market": market.upper(),
                "symbol": symbol,
                "code": symbol,
                "tdx_code": row.get("tdx_code") or f"{market.lower()}{symbol}",
                "trade_date": trade_date,
                "trade_time": datetime.combine(trade_date, SESSION_SLOTS[slot]),
                "time_label": SESSION_SLOTS[slot].strftime("%H:%M"),
                "slot": slot,
                "price": round(price, 4),
                "avg_price": _number(row.get("avg_price") or row.get("average_price") or row.get("avg")),
                "volume": volume,
                "turnover": _number(row.get("turnover") or row.get("amount"), default=0.0),
                "source": source,
            }
        )
    normalized.sort(key=lambda item: int(item["slot"]))
    deduped: dict[int, dict[str, object]] = {int(item["slot"]): item for item in normalized}
    return [deduped[index] for index in sorted(deduped)]


def enrich_with_minute_amount(
    rows: Sequence[Mapping[str, object]],
    minute_amounts: Mapping[int, Mapping[str, object]],
    *,
    volume_multiplier: int = 100,
) -> list[dict[str, object]]:
    cumulative_amount = 0.0
    cumulative_shares = 0.0
    result: list[dict[str, object]] = []
    for row in rows:
        item = dict(row)
        slot = int(item["slot"])
        kline = minute_amounts.get(slot)
        amount = _number(kline.get("amount")) if kline else None
        shares = _number(kline.get("volume")) if kline else None
        if amount is not None:
            item["turnover"] = amount
            cumulative_amount += amount
        if shares is not None and shares > 0:
            cumulative_shares += shares
        elif item.get("volume") is not None:
            cumulative_shares += float(item["volume"] or 0) * volume_multiplier
        if cumulative_shares > 0 and cumulative_amount > 0:
            item["avg_price"] = round(cumulative_amount / cumulative_shares, 4)
        result.append(item)
    return result


def select_top_bottom_industry_targets(
    rows: Iterable[Mapping[str, object]],
    *,
    limit_symbols: set[str] | None = None,
    top_n: int = 15,
    bottom_n: int = 10,
) -> list[tuple[str, str, int, str]]:
    excluded = {str(value).strip() for value in (limit_symbols or set())}
    candidates = [
        row for row in rows
        if str(row.get("stock_code") or row.get("code") or "").strip() not in excluded
        and _number(row.get("change_pct") or row.get("changePct")) is not None
    ]
    candidates.sort(key=lambda row: (_number(row.get("change_pct") or row.get("changePct")) or 0, str(row.get("stock_code") or row.get("code"))))
    selected: list[tuple[str, str, int, str]] = []
    seen: set[str] = set()
    for direction, side in (("DECLINER", candidates[: max(0, bottom_n)]), ("GAINER", reversed(candidates[-max(0, top_n):]))):
        for position, row in enumerate(side, start=1):
            code = str(row.get("stock_code") or row.get("code") or "").strip()[-6:].zfill(6)
            if not code or code in seen:
                continue
            seen.add(code)
            selected.append((code, direction, position, str(row.get("industry_code") or "")))
    return selected


def _datetime(value: object) -> datetime | None:
    if isinstance(value, datetime):
        return value
    if isinstance(value, date):
        return datetime.combine(value, time.min)
    if value in (None, ""):
        return None
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00")).replace(tzinfo=None)
    except ValueError:
        return None


def _number(value: object, *, default: float | None = None) -> float | None:
    if value in (None, "", "--", "-"):
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default
