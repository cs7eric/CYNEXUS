from __future__ import annotations

from collections import defaultdict
from datetime import datetime
from typing import Any


EVENT_TYPES = {
    "rapid_rise",
    "rapid_fall",
    "limit_up_touch",
    "limit_up_sealed",
    "limit_up_opened",
    "limit_down_touch",
    "limit_down_sealed",
    "limit_down_opened",
}


def select_indicator_candidates(
    metadata: dict[str, dict[str, Any]],
    previous_snapshots: dict[str, dict[str, Any]],
    *,
    captured_at: datetime,
    candidate_limit: int = 800,
    hot_limit: int = 500,
    rotation_limit: int = 300,
    rotation_buckets: int = 8,
    rotation_index: int | None = None,
) -> dict[str, dict[str, Any]]:
    """Select a small intraday pool while keeping a deterministic cold-stock rotation.

    The hot part follows the last known market state and event status. The rotation
    part prevents a quiet stock from disappearing forever without requesting quotes
    for the whole security master on every tick.
    """
    if not metadata:
        return {}
    limit = max(1, candidate_limit)
    hot = sorted(
        metadata.items(),
        key=lambda item: _candidate_score(item[0], previous_snapshots.get(item[0])),
        reverse=True,
    )[: max(0, hot_limit)]
    selected: list[tuple[str, dict[str, Any], str]] = [
        (code, info, "hot_signal") for code, info in hot
    ]
    selected_codes = {code for code, _, _ in selected}
    buckets = max(1, rotation_buckets)
    bucket = (rotation_index if rotation_index is not None else captured_at.minute // 5) % buckets
    cold_candidates = [
        (code, info)
        for code, info in sorted(metadata.items())
        if code not in selected_codes and _rotation_bucket(code, buckets) == bucket
    ]
    selected.extend((code, info, "rotation") for code, info in cold_candidates[: max(0, rotation_limit)])
    if len(selected) < limit:
        for code, info in metadata.items():
            if code not in {item[0] for item in selected}:
                selected.append((code, info, "coverage"))
                if len(selected) >= limit:
                    break
    selected = selected[:limit]
    result: dict[str, dict[str, Any]] = {}
    for code, info, reason in selected:
        enriched = dict(info)
        enriched["candidate_tier"] = "hot" if reason == "hot_signal" else "rotation"
        enriched["candidate_reason"] = reason
        enriched["candidate_score"] = round(_candidate_score(code, previous_snapshots.get(code)), 6)
        result[code] = enriched
    return result


def _candidate_score(code: str, snapshot: dict[str, Any] | None) -> float:
    if not snapshot:
        return 0.0
    change = abs(_number(snapshot.get("change_pct")))
    amount = _number(snapshot.get("amount"))
    turnover = _number(snapshot.get("turnover_rate"))
    limit_status = str(snapshot.get("limit_status") or "")
    limit_bonus = 100.0 if limit_status != "normal" else 0.0
    return limit_bonus + min(change, 20.0) * 3.0 + min(turnover, 30.0) + min(amount / 100_000_000.0, 30.0)


def _rotation_bucket(code: str, buckets: int) -> int:
    try:
        return int(str(code)[-6:]) % buckets
    except (TypeError, ValueError):
        return 0


def _number(value: Any) -> float:
    try:
        return float(value or 0)
    except (TypeError, ValueError):
        return 0.0


def normalize_indicator_quote(
    quote: dict[str, Any],
    *,
    code: str,
    name: str | None,
    market: str | None,
    captured_at: datetime,
    limit_up_threshold: float = 10.0,
    float_shares: float | None = None,
) -> dict[str, Any]:
    last = _float(_first(quote, "last_price", "lastPrice", "price"))
    # TDX uses zero as a placeholder while a quote is unavailable. Treat it
    # as missing so it cannot become a synthetic -100% move.
    if last is not None and last <= 0:
        last = None
    previous = _float(_first(quote, "pre_close_price", "preClosePrice", "pre_close"))
    change_pct = _float(_first(quote, "change_pct", "changePct"))
    if last is None:
        change_pct = None
    elif change_pct is None and previous is not None and previous > 0:
        change_pct = (last - previous) / previous * 100
    threshold = _threshold_ratio(limit_up_threshold)
    limit_up_price = _float(_first(quote, "limit_up_price", "limitUpPrice"))
    limit_down_price = _float(_first(quote, "limit_down_price", "limitDownPrice"))
    if limit_up_price is not None and limit_up_price <= 0:
        limit_up_price = None
    if limit_down_price is not None and limit_down_price <= 0:
        limit_down_price = None
    if limit_up_price is None and previous is not None and previous > 0:
        limit_up_price = _round_price(previous * (1 + threshold))
    if limit_down_price is None and previous is not None and previous > 0:
        limit_down_price = _round_price(previous * (1 - threshold))
    high_price = _float(_first(quote, "high_price", "highPrice"))
    low_price = _float(_first(quote, "low_price", "lowPrice"))
    volume = _float(_first(quote, "total_hand", "totalHand", "volume"))
    turnover_rate = _float(_first(quote, "turnover_rate", "turnoverRate"))
    if turnover_rate is None and float_shares and float_shares > 0 and volume is not None:
        turnover_rate = volume * 10000 / float_shares
    status = _limit_status(
        last,
        high_price,
        low_price,
        limit_up_price,
        limit_down_price,
        quote,
    )
    return {
        "code": _plain_code(code),
        "name": name or _first(quote, "name", "stock_name"),
        "industry": _first(quote, "industry", "industry_name"),
        "market": (market or _market_from_code(code)).upper(),
        "last_price": last,
        "prev_close": previous,
        "open_price": _float(_first(quote, "open_price", "openPrice")),
        "high_price": high_price,
        "low_price": low_price,
        "change_pct": round(change_pct, 6) if change_pct is not None else None,
        "amount": _float(_first(quote, "amount", "turnover")),
        "volume": volume,
        "turnover_rate": turnover_rate,
        "limit_up_price": limit_up_price,
        "limit_down_price": limit_down_price,
        "limit_status": status,
        "captured_at": captured_at,
        "raw_payload": quote,
    }


def build_indicator_industry_snapshots(
    rows: list[dict[str, Any]],
    events: list[dict[str, Any]],
    industry_by_code: dict[str, dict[str, Any]],
    *,
    captured_at: datetime | None = None,
) -> list[dict[str, Any]]:
    """Build one persisted industry summary for the current quote capture."""
    groups: dict[tuple[str, str], dict[str, Any]] = {}
    for row in rows:
        industry_code, industry_name = _resolve_industry(row, industry_by_code)
        key = (industry_code or "", industry_name)
        group = groups.setdefault(key, _new_industry_group(industry_code, industry_name))
        group["stock_count"] += 1
        change = row.get("change_pct")
        if change is not None and float(change) > 0:
            group["advancing_count"] += 1
        elif change is not None and float(change) < 0:
            group["declining_count"] += 1
        else:
            group["flat_count"] += 1
        status = row.get("limit_status")
        if status == "limit_up_sealed":
            group["limit_up_count"] += 1
        elif status == "limit_up_touch":
            group["limit_up_touch_count"] += 1
        elif status == "limit_down_sealed":
            group["limit_down_count"] += 1
        elif status == "limit_down_touch":
            group["limit_down_touch_count"] += 1

    event_groups: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    for event in events:
        snapshot = event.get("snapshot") or {}
        industry_code, industry_name = _resolve_industry(snapshot, industry_by_code)
        event_groups[(industry_code or "", industry_name)].append(event)
        group = groups.setdefault(
            (industry_code or "", industry_name),
            _new_industry_group(industry_code, industry_name),
        )
        event_type = event.get("event_type")
        if event_type == "rapid_rise":
            group["rapid_rise_count"] += 1
        elif event_type == "rapid_fall":
            group["rapid_fall_count"] += 1

    resolved_capture = captured_at or (rows[0].get("captured_at") if rows else None)
    if resolved_capture is None:
        return []
    for key, group in groups.items():
        industry_events = sorted(event_groups.get(key, []), key=lambda item: item.get("event_at"))
        if industry_events:
            first = industry_events[0]
            snapshot = first.get("snapshot") or {}
            group.update({
                "first_event_at": first.get("event_at"),
                "first_event_code": first.get("code"),
                "first_event_name": first.get("name"),
                "first_event_type": first.get("event_type"),
                "first_event_signal_value": first.get("signal_value"),
            })
        limit_events = [
            item for item in industry_events
            if item.get("event_type") in {"limit_up_touch", "limit_up_sealed"}
        ]
        if limit_events:
            first_limit = limit_events[0]
            snapshot = first_limit.get("snapshot") or {}
            group.update({
                "first_limit_up_at": first_limit.get("event_at"),
                "first_limit_up_code": first_limit.get("code"),
                "first_limit_up_name": first_limit.get("name"),
                "first_limit_up_change_pct": snapshot.get("change_pct"),
            })
        group["trade_date"] = resolved_capture.date()
        group["captured_at"] = resolved_capture
    return list(groups.values())


def _new_industry_group(industry_code: str | None, industry_name: str) -> dict[str, Any]:
    return {
        "industry_code": industry_code,
        "industry_name": industry_name,
        "stock_count": 0,
        "advancing_count": 0,
        "declining_count": 0,
        "flat_count": 0,
        "limit_up_count": 0,
        "limit_up_touch_count": 0,
        "limit_down_count": 0,
        "limit_down_touch_count": 0,
        "rapid_rise_count": 0,
        "rapid_fall_count": 0,
        "first_limit_up_at": None,
        "first_limit_up_code": None,
        "first_limit_up_name": None,
        "first_limit_up_change_pct": None,
        "first_event_at": None,
        "first_event_code": None,
        "first_event_name": None,
        "first_event_type": None,
        "first_event_signal_value": None,
    }


def _resolve_industry(
    row: dict[str, Any],
    industry_by_code: dict[str, dict[str, Any]],
) -> tuple[str | None, str]:
    code = _plain_code(row.get("code"))
    metadata = industry_by_code.get(code) or {}
    industry_name = str(metadata.get("industry_name") or "未分类").strip()
    industry_code = metadata.get("industry_code") or metadata.get("sector_id")
    return (str(industry_code).strip() if industry_code else None), industry_name or "未分类"


def detect_indicator_events(
    current: dict[str, Any],
    previous: dict[str, Any] | None,
    *,
    rapid_change_pct: float = 1.0,
    max_gap_minutes: int = 10,
) -> list[dict[str, Any]]:
    if not current.get("code") or current.get("change_pct") is None:
        return []
    current_status = current.get("limit_status")
    previous_status = (previous or {}).get("limit_status")
    events: list[dict[str, Any]] = []
    change = float(current["change_pct"])
    previous_change = float((previous or {}).get("change_pct") or 0)
    if _rapid_comparison_is_valid(current, previous, max_gap_minutes):
        delta = change - previous_change
        if delta >= rapid_change_pct:
            events.append(_event(current, "rapid_rise", delta))
        if delta <= -rapid_change_pct:
            events.append(_event(current, "rapid_fall", delta))
    transitions = {
        (None, "limit_up_touch"): "limit_up_touch",
        (None, "limit_up_sealed"): "limit_up_sealed",
        ("normal", "limit_up_touch"): "limit_up_touch",
        ("normal", "limit_up_sealed"): "limit_up_sealed",
        ("limit_up_touch", "limit_up_sealed"): "limit_up_sealed",
        ("limit_up_sealed", "limit_up_touch"): "limit_up_opened",
        (None, "limit_down_touch"): "limit_down_touch",
        (None, "limit_down_sealed"): "limit_down_sealed",
        ("normal", "limit_down_touch"): "limit_down_touch",
        ("normal", "limit_down_sealed"): "limit_down_sealed",
        ("limit_down_touch", "limit_down_sealed"): "limit_down_sealed",
        ("limit_down_sealed", "limit_down_touch"): "limit_down_opened",
    }
    event_type = transitions.get((previous_status, current_status))
    if event_type:
        events.append(_event(current, event_type, change))
    return events


def _event(current: dict[str, Any], event_type: str, signal_value: float) -> dict[str, Any]:
    return {
        "code": current["code"],
        "name": current.get("name"),
        "market": current.get("market"),
        "event_type": event_type,
        "event_at": current["captured_at"],
        "signal_value": round(signal_value, 6),
        "snapshot": current,
    }


def _limit_status(
    last: float | None,
    high: float | None,
    low: float | None,
    limit_up_price: float | None,
    limit_down_price: float | None,
    quote: dict[str, Any],
) -> str:
    if limit_up_price is not None and _at_price(last, limit_up_price):
        return "limit_up_sealed" if _book_is_empty(quote, "sell_levels", "asks") else "limit_up_touch"
    if limit_up_price is not None and _at_price(high, limit_up_price):
        return "limit_up_touch"
    if limit_down_price is not None and _at_price(last, limit_down_price):
        return "limit_down_sealed" if _book_is_empty(quote, "buy_levels", "bids") else "limit_down_touch"
    if limit_down_price is not None and _at_price(low, limit_down_price):
        return "limit_down_touch"
    return "normal"


def _threshold_ratio(value: float) -> float:
    return value if 0 < value <= 1 else value / 100


def _round_price(value: float) -> float:
    return round(value + 1e-9, 2)


def _at_price(value: float | None, target: float, tolerance: float = 0.011) -> bool:
    return value is not None and abs(value - target) <= max(tolerance, abs(target) * 0.0005)


def _book_is_empty(quote: dict[str, Any], *names: str) -> bool:
    levels = _first(quote, *names)
    if levels is None:
        # A quote without an order book cannot prove an open board; exact limit
        # price is therefore retained as a conservative sealed observation.
        return True
    if isinstance(levels, dict):
        levels = levels.get("items") or levels.get("levels") or list(levels.values())
    if not isinstance(levels, (list, tuple)):
        return False
    for level in levels:
        if isinstance(level, dict):
            volume = _float(_first(level, "volume", "vol", "quantity"))
        elif isinstance(level, (list, tuple)) and len(level) > 1:
            volume = _float(level[1])
        else:
            volume = _float(getattr(level, "volume", None) or getattr(level, "vol", None))
        if volume is not None and volume > 0:
            return False
    return True


def _rapid_comparison_is_valid(
    current: dict[str, Any], previous: dict[str, Any] | None, max_gap_minutes: int,
) -> bool:
    if previous is None or previous.get("change_pct") is None:
        return False
    current_at = current.get("captured_at")
    previous_at = previous.get("captured_at")
    if not isinstance(current_at, datetime) or not isinstance(previous_at, datetime):
        return True
    gap = (current_at - previous_at).total_seconds() / 60
    return 0 <= gap <= max(1, max_gap_minutes)


def _first(value: dict[str, Any], *names: str) -> Any:
    for name in names:
        if value.get(name) not in (None, ""):
            return value[name]
    return None


def _float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", ""))
    except (TypeError, ValueError):
        return None


def _plain_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        text = text[2:]
    return text.zfill(6)


def _market_from_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    return {"6": "SH", "0": "SZ", "3": "SZ", "8": "BJ", "4": "BJ"}.get(text[:1], "")
