"""Pure Trade Review market-state projections.

The collector owns raw facts.  This module deliberately contains only
deterministic transformations so a replay cursor can be reproduced from the
same 1m bars and persisted Indicator events without contacting a provider.
"""
from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Iterable, Mapping
from zoneinfo import ZoneInfo


LIMIT_UP_EVENTS = {"limit_up_touch", "limit_up_sealed", "limit_up_opened"}
LIMIT_DOWN_EVENTS = {"limit_down_touch", "limit_down_sealed", "limit_down_opened"}


@dataclass(frozen=True)
class MinuteProjection:
    minute_at: datetime
    market: dict[str, Any]
    industries: list[dict[str, Any]]
    pools: list[dict[str, Any]]


def normalize_indicator_events(rows: Iterable[dict[str, Any]], contexts: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    """Map legacy Indicator names to the replay vocabulary without dropping evidence."""
    aliases = {
        "limit_up_sealed": "LIMIT_UP_SEAL",
        "limit_up_touch": "LIMIT_UP_TOUCH",
        "limit_up_opened": "LIMIT_UP_OPEN",
        "limit_down_sealed": "LIMIT_DOWN_SEAL",
        "limit_down_touch": "LIMIT_DOWN_TOUCH",
        "limit_down_opened": "LIMIT_DOWN_OPEN",
        "rapid_rise": "RAPID_RISE",
        "rapid_fall": "RAPID_DROP",
    }
    output: list[dict[str, Any]] = []
    seen: set[tuple[Any, ...]] = set()
    for raw in rows:
        event_at = raw.get("event_at") or raw.get("eventAt")
        code = _code(raw.get("code"))
        if not code or not isinstance(event_at, datetime):
            continue
        event_type = aliases.get(str(raw.get("event_type") or raw.get("eventType") or "").lower(), "INDICATOR")
        key = (code, event_type, event_at)
        if key in seen:
            continue
        seen.add(key)
        context = contexts.get(code, {})
        payload = raw.get("payload") or {}
        if not isinstance(payload, dict):
            payload = {"raw": payload}
        output.append({
            "code": code,
            "name": raw.get("name") or context.get("name"),
            "event_type": event_type,
            "event_at": event_at,
            "effective_minute": event_at.replace(second=0, microsecond=0),
            "price": _number(raw.get("last_price") or raw.get("price")),
            "change_pct": _number(raw.get("change_pct") or raw.get("changePct")),
            "limit_up_price": context.get("limit_up_price"),
            "limit_down_price": context.get("limit_down_price"),
            "source": raw.get("source") or "market_indicator",
            # Membership is frozen with the event so a historical replay does
            # not silently inherit today's THS/TDX constituent relation.
            "memberships": context.get("memberships") or [],
            "payload": {**payload, "memberships": context.get("memberships") or []},
        })
    return sorted(output, key=lambda item: (item["event_at"], item["code"], item["event_type"]))


def build_limit_intervals(events: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    """Build compact focused-stock state intervals from standardized events."""
    state_by_code: dict[str, tuple[str, datetime, int]] = {}
    output: list[dict[str, Any]] = []
    for event in sorted(events, key=lambda item: (item["event_at"], item["code"])):
        code = event["code"]
        event_type = event["event_type"]
        next_state = {
            "LIMIT_UP_SEAL": "SEALED",
            "LIMIT_UP_OPEN": "OPENED",
            "LIMIT_DOWN_SEAL": "LIMIT_DOWN",
            "LIMIT_DOWN_OPEN": "LIMIT_DOWN_OPENED",
        }.get(event_type)
        if next_state is None:
            continue
        previous = state_by_code.get(code)
        if previous and previous[0] == next_state:
            state_by_code[code] = (previous[0], previous[1], previous[2] + 1)
            continue
        if previous:
            output.append({"code": code, "state": previous[0], "started_at": previous[1], "ended_at": event["event_at"], "event_count": previous[2]})
        state_by_code[code] = (next_state, event["event_at"], 1)
    for code, (state, started_at, event_count) in state_by_code.items():
        output.append({"code": code, "state": state, "started_at": started_at, "ended_at": None, "event_count": event_count})
    return output


def build_minute_projections(
    bars: Iterable[dict[str, Any]],
    contexts: dict[str, dict[str, Any]],
    *,
    memberships: tuple[str, ...] = ("ths", "tdx", "union"),
) -> list[MinuteProjection]:
    """Derive cursor-safe market and industry snapshots from completed 1m bars."""
    by_minute: dict[datetime, list[dict[str, Any]]] = defaultdict(list)
    for raw in bars:
        code = _code(raw.get("code") or raw.get("symbol"))
        minute = raw.get("trade_time") or raw.get("minute_at")
        if code and isinstance(minute, datetime) and code in contexts:
            by_minute[minute].append({**raw, "code": code})

    touched: set[str] = set()
    # These state facts are updated strictly in minute order. They belong on
    # the pool payload so an intraday replay can show a first seal and the
    # number of subsequent breaks without reading a future daily summary.
    first_seal_at: dict[str, datetime] = {}
    open_count: dict[str, int] = defaultdict(int)
    was_sealed: dict[str, bool] = {}
    # A raw 1m series can omit a minute when a security has no transaction.
    # Replay state must use the last completed bar at the cursor, not shrink
    # the market universe to only securities traded in that exact minute.
    latest_by_code: dict[str, dict[str, Any]] = {}
    output: list[MinuteProjection] = []
    for minute_at in sorted(by_minute):
        for bar in by_minute[minute_at]:
            latest_by_code[bar["code"]] = bar
        rows = list(latest_by_code.values())
        stock_rows: list[dict[str, Any]] = []
        for bar in rows:
            context = contexts[bar["code"]]
            if context.get("security_type") != "STOCK":
                continue
            pre_close = _number(context.get("pre_close"))
            close = _number(bar.get("close"))
            high = _number(bar.get("high"))
            if pre_close is None or pre_close <= 0 or close is None:
                continue
            up = _number(context.get("limit_up_price"))
            down = _number(context.get("limit_down_price"))
            if up is not None and high is not None and high >= up - 0.00001:
                touched.add(bar["code"])
            change = (close - pre_close) / pre_close * 100
            sealed = up is not None and close >= up - 0.00001
            down_sealed = down is not None and close <= down + 0.00001
            stock_rows.append({
                "code": bar["code"], "name": context.get("name"), "change_pct": change,
                "close": close, "sealed": sealed, "down_sealed": down_sealed,
                "touched": bar["code"] in touched, "height": int(context.get("previous_streak") or 0) + 1 if sealed else 0,
                "previous_limit_status": context.get("previous_limit_status"),
                "memberships": context.get("memberships") or [],
            })
        for stock in stock_rows:
            code = stock["code"]
            if stock["sealed"] and code not in first_seal_at:
                first_seal_at[code] = minute_at
            if was_sealed.get(code, False) and not stock["sealed"]:
                open_count[code] += 1
            was_sealed[code] = stock["sealed"]
            stock["first_seal_at"] = first_seal_at.get(code)
            stock["open_count"] = open_count[code]
        market = _snapshot(stock_rows, minute_at, contexts)
        industries = _industry_snapshots(stock_rows, minute_at, market, memberships)
        pools = _pool_indexes(stock_rows, minute_at, memberships)
        output.append(MinuteProjection(minute_at=minute_at, market=market, industries=industries, pools=pools))
    return output


def build_daily_projection(
    projections: list[MinuteProjection], previous_contexts: dict[str, dict[str, Any]],
    events: Iterable[dict[str, Any]] = (),
    *,
    current_limit_rows: Mapping[str, Mapping[str, Any]] | None = None,
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    if not projections:
        return {}, []
    final = dict(projections[-1].market)
    final["promotion_rate"] = _promotion_rate(projections[-1].pools, previous_contexts)
    final["break_streak_rate"] = _break_streak_rate(projections[-1].pools, previous_contexts)
    has_authoritative_limit_rows = any(
        str(row.get("category") or "").strip().lower() in {"limit_up", "limit_down", "broken"}
        for row in (current_limit_rows or {}).values()
    )
    final["source"] = {
        "algorithmVersion": "daily-v2",
        "sourceType": "postgres.market_limit_daily_stocks+clickhouse_1m+market_indicator"
        if has_authoritative_limit_rows else "clickhouse_1m+market_indicator",
        "available": True,
        "qualityStatus": "ready",
    }
    summaries = _daily_stock_summaries(projections, previous_contexts, events)
    daily_pools: list[dict[str, Any]] = []
    for pool in projections[-1].pools:
        if pool["pool_type"] in {"sealed", "broken", "limit_down", "ladder", "yesterday_limit_up_feedback", "yesterday_limit_down_feedback"}:
            daily_pools.append({
                **pool,
                "pool_type": {"sealed": "limit_up", "limit_down": "limit_down"}.get(pool["pool_type"], pool["pool_type"]),
                "stocks": [_enrich_daily_stock(stock, summaries.get(stock["code"], {})) for stock in pool["stocks"]],
            })
    if has_authoritative_limit_rows:
        # The daily limit table is the same as the source used by the market
        # pulse. Raw 1m bars remain the replay source, but they can miss a
        # sealed close or carry a slightly different limit-price rounding;
        # use the persisted daily classification for the post-close pools.
        authoritative = _authoritative_daily_limit_pools(current_limit_rows, summaries, previous_contexts)
        daily_pools = [
            pool for pool in daily_pools
            if not (
                pool.get("membership") == "union"
                and not pool.get("industry_code")
                and pool.get("pool_type") in {"limit_up", "ladder", "limit_down"}
            )
        ]
        daily_pools.extend(authoritative["pools"])
        final.update(authoritative["market"])
    # These two pools are not mutually exclusive with the final sealed pool;
    # they describe intraday process and yesterday-continuation feedback.
    daily_pools.extend(_process_daily_pools(summaries, previous_contexts))
    # Keep the industry leaders as a compact, durable pool rather than making
    # the daily Panel reconstruct it from all constituent minute rows.
    for industry in projections[-1].industries:
        stocks = [
            _enrich_daily_stock(stock, summaries.get(stock["code"], {}))
            for stock in industry.get("core_pool") or []
        ]
        if stocks:
            daily_pools.append({
                "pool_type": "industry_core",
                "membership": industry["membership"],
                "industry_code": industry["industry_code"],
                "stocks": stocks,
            })
    # A daily replay must have the same scoped pools as the minute replay.
    # The final minute is only the *input* for the post-close fact; the
    # durable daily pool is independently keyed by membership/industry so a
    # historical day cursor never has to reconstruct it in the browser.
    daily_pools.extend(_scoped_daily_pools(daily_pools, contexts=previous_contexts))
    return final, daily_pools


def _authoritative_daily_limit_pools(
    rows: Mapping[str, Mapping[str, Any]],
    summaries: Mapping[str, Mapping[str, Any]],
    contexts: Mapping[str, Mapping[str, Any]],
) -> dict[str, Any]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for raw in rows.values():
        category = str(raw.get("category") or "").strip().lower()
        if category not in {"limit_up", "limit_down", "broken"}:
            continue
        code = _code(raw.get("code"))
        if not code:
            continue
        context = contexts.get(code, {})
        summary = summaries.get(code, {})
        streak = int(_number(raw.get("streak")) or 0) if category == "limit_up" else 0
        stock = _enrich_daily_stock({
            "code": code,
            "name": raw.get("name") or context.get("name") or code,
            "changePct": _number(raw.get("change_pct")) if raw.get("change_pct") is not None else None,
            "height": streak,
            "sealed": category == "limit_up",
        }, summary)
        # Daily classification and streak are authoritative even when the
        # derived summary has a different last-minute view.
        stock["name"] = raw.get("name") or stock.get("name") or code
        stock["height"] = streak
        stock["sealed"] = category == "limit_up"
        if raw.get("change_pct") is not None:
            stock["changePct"] = _number(raw.get("change_pct"))
        grouped[category].append(stock)

    limit_up = sorted(grouped["limit_up"], key=lambda item: (item.get("height", 0), item.get("changePct") or 0), reverse=True)
    limit_down = sorted(grouped["limit_down"], key=lambda item: item.get("changePct") or 0)
    broken = grouped["broken"]
    touched_count = len(limit_up) + len(broken)
    sealed_count = len(limit_up)
    market = {
        "touched_limit_up_count": touched_count,
        "sealed_limit_up_count": sealed_count,
        "broken_limit_up_count": len(broken),
        "sealed_limit_down_count": len(limit_down),
        "max_height": max((item.get("height", 0) for item in limit_up), default=0),
        "seal_rate": _rate(sealed_count, touched_count),
        "break_rate": _rate(len(broken), touched_count),
        "ladder": [
            {"height": height, "count": sum(1 for item in limit_up if item.get("height") == height)}
            for height in sorted({item.get("height", 0) for item in limit_up if item.get("height", 0) > 0}, reverse=True)
        ],
    }
    return {
        "market": market,
        "pools": [
            {"pool_type": "limit_up", "membership": "union", "industry_code": "", "stocks": limit_up},
            {"pool_type": "ladder", "membership": "union", "industry_code": "", "stocks": [item for item in limit_up if item.get("height", 0) > 0]},
            {"pool_type": "limit_down", "membership": "union", "industry_code": "", "stocks": limit_down},
        ],
    }


def build_security_daily_facts(
    bars: Iterable[dict[str, Any]], contexts: dict[str, dict[str, Any]], events: Iterable[dict[str, Any]] = (),
) -> list[dict[str, Any]]:
    """Build one immutable end-of-day process fact per replay security.

    This deliberately consumes raw 1m bars instead of a final market
    snapshot.  It means ordinary securities, ETFs and one-word boards retain
    usable daily facts even when they never appear in an Indicator event.
    """
    by_code: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for raw in bars:
        code = _code(raw.get("code") or raw.get("symbol"))
        minute = raw.get("trade_time") or raw.get("minute_at")
        if code in contexts and isinstance(minute, datetime):
            by_code[code].append({**raw, "code": code, "minute_at": minute})
    event_by_code: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for event in events:
        code = _code(event.get("code"))
        if code in contexts:
            event_by_code[code].append(event)

    facts: list[dict[str, Any]] = []
    for code, context in contexts.items():
        rows = sorted(by_code.get(code, []), key=lambda item: item["minute_at"])
        if not rows:
            continue
        closes = [_number(item.get("close")) for item in rows]
        highs = [_number(item.get("high")) for item in rows]
        lows = [_number(item.get("low")) for item in rows]
        valid_closes = [value for value in closes if value is not None]
        valid_highs = [value for value in highs if value is not None]
        valid_lows = [value for value in lows if value is not None]
        if not valid_closes:
            continue
        pre_close = _number(context.get("pre_close"))
        close = valid_closes[-1]
        high = max(valid_highs or valid_closes)
        low = min(valid_lows or valid_closes)
        limit_up = _number(context.get("limit_up_price"))
        limit_down = _number(context.get("limit_down_price"))
        emotion_eligible = context.get("security_type") == "STOCK"
        touched_up = emotion_eligible and limit_up is not None and high >= limit_up - 0.00001
        touched_down = emotion_eligible and limit_down is not None and low <= limit_down + 0.00001
        final_up = emotion_eligible and limit_up is not None and close >= limit_up - 0.00001
        final_down = emotion_eligible and limit_down is not None and close <= limit_down + 0.00001
        sealed_series = [bool(emotion_eligible and limit_up is not None and value is not None and value >= limit_up - 0.00001) for value in closes]
        first_seal_at = next((row["minute_at"] for row, sealed in zip(rows, sealed_series, strict=True) if sealed), None)
        first_down_at = next((row["minute_at"] for row, value in zip(rows, closes, strict=True) if emotion_eligible and limit_down is not None and value is not None and value <= limit_down + 0.00001), None)
        open_count = sum(1 for before, after in zip(sealed_series, sealed_series[1:], strict=False) if before and not after)
        reseal_count = sum(1 for before, after in zip(sealed_series, sealed_series[1:], strict=False) if not before and after) - (1 if sealed_series and sealed_series[0] else 0)
        # Indicator records have second precision. They can move the first
        # seal earlier than the containing 1m bar, but never invent a final
        # market state for a stock not supported by the raw bar series.
        indicator_events = sorted(event_by_code.get(code, []), key=lambda item: _datetime_order(item.get("event_at")))
        indicator_seals = [item.get("event_at") for item in indicator_events if item.get("event_type") == "LIMIT_UP_SEAL" and isinstance(item.get("event_at"), datetime)]
        if indicator_seals:
            first_seal_at = min(
                [value for value in [first_seal_at, *indicator_seals] if value is not None],
                key=_datetime_order,
            )
        change_pct = (close - pre_close) / pre_close * 100 if pre_close and pre_close > 0 else None
        amplitude = (high - low) / pre_close * 100 if pre_close and pre_close > 0 else None
        volume = sum(_number(item.get("volume")) or 0.0 for item in rows)
        amount = sum(_number(item.get("amount")) or 0.0 for item in rows)
        previous_streak = int(context.get("previous_streak") or 0)
        is_one_word = bool(final_up and abs(high - low) < 0.00001 and abs(close - high) < 0.00001)
        opening_price = _number(rows[0].get("open")) or valid_closes[0]
        facts.append({
            "code": code, "name": context.get("name"), "security_type": context.get("security_type"),
            "open": opening_price, "high": high, "low": low, "close": close,
            "change_pct": round(change_pct, 6) if change_pct is not None else None,
            "amplitude": round(amplitude, 6) if amplitude is not None else None,
            "volume": volume, "amount": amount,
            "touch_limit_up": touched_up, "touch_limit_down": touched_down,
            "final_limit_up": final_up, "final_limit_down": final_down,
            "first_limit_up_at": first_seal_at, "first_limit_down_at": first_down_at,
            "open_count": max(0, open_count), "reseal_count": max(0, reseal_count),
            "previous_streak": previous_streak,
            "current_streak": previous_streak + 1 if final_up else 0,
            "is_advanced": bool(final_up and previous_streak > 0),
            "is_broken_streak": bool(emotion_eligible and previous_streak > 0 and not final_up),
            "previous_limit_status": context.get("previous_limit_status"),
            "intraday_available": True, "is_one_word_limit": is_one_word,
            "event_evidence_quality": "indicator_refined" if indicator_events else "derived_1m",
            "memberships": context.get("memberships") or [],
        })
    return facts


def classify_phase(rows: list[dict[str, Any]], *, window_days: int = 60) -> dict[str, Any]:
    """Classify a strictly as-of daily series with an explicit policy contract.

    ``rows`` is expected to contain only completed trading days at or before
    the replay date.  The coverage fields are deliberately persisted with the
    result so an incomplete historical backfill is shown as a degradation,
    rather than being presented as a full-window market conclusion.
    """
    expected = max(1, int(window_days))
    coverage = min(1.0, len(rows) / expected)
    if not rows:
        return {
            "phase": "未知", "phase_score": 0.0,
            "factor": {"reason": "no_daily_state", "dataCoverage": 0.0},
            "windowDays": expected, "policyVersion": "daily-v2", "dataCoverage": 0.0,
        }
    recent = rows[-5:]
    last = recent[-1]
    seal = _number(last.get("seal_rate")) or 0.0
    broken = _number(last.get("break_rate")) or 0.0
    up = int(last.get("sealed_limit_up_count") or 0)
    down = int(last.get("sealed_limit_down_count") or 0)
    height = int(last.get("max_height") or 0)
    score = seal * 0.45 - broken * 0.25 + min(height, 10) * 3 + min(up, 100) * 0.12 - min(down, 100) * 0.2
    previous_height = int(recent[-2].get("max_height") or 0) if len(recent) > 1 else height
    if down > up or (broken >= 45 and seal < 45):
        phase = "退潮"
    elif broken >= 35:
        phase = "分歧"
    elif height >= 6 and seal >= 65:
        phase = "高潮"
    elif height > previous_height and seal >= 55:
        phase = "加速"
    elif seal >= 50 and up > 0:
        phase = "启动"
    else:
        phase = "回流"
    return {
        "phase": phase,
        "phase_score": round(score, 4),
        "factor": {
            "sealRate": seal, "breakRate": broken, "limitUp": up,
            "limitDown": down, "maxHeight": height,
            "previousMaxHeight": previous_height,
            "dataCoverage": round(coverage, 4),
        },
        "windowDays": expected,
        "policyVersion": "daily-v2",
        "dataCoverage": round(coverage, 4),
    }


def _snapshot(rows: list[dict[str, Any]], minute_at: datetime, contexts: Mapping[str, Mapping[str, Any]] | None = None) -> dict[str, Any]:
    up = sum(1 for item in rows if item["change_pct"] > 0)
    down = sum(1 for item in rows if item["change_pct"] < 0)
    touched = [item for item in rows if item["touched"]]
    sealed = [item for item in rows if item["sealed"]]
    broken = [item for item in touched if not item["sealed"]]
    ladder: dict[int, int] = defaultdict(int)
    for item in sealed:
        if item["height"]:
            ladder[item["height"]] += 1
    context_by_code = contexts or {}
    eligible = [
        context for context in context_by_code.values()
        if int(context.get("previous_streak") or 0) > 0 and context.get("security_type") == "STOCK"
    ]
    promoted = sum(
        1 for item in sealed
        if int(context_by_code.get(item["code"], {}).get("previous_streak") or 0) > 0
    )
    changes = sorted(item["change_pct"] for item in rows)
    middle = len(changes) // 2
    median_change = (
        None if not changes
        else changes[middle] if len(changes) % 2
        else (changes[middle - 1] + changes[middle]) / 2
    )
    return {
        "minute_at": minute_at, "advancing_count": up, "declining_count": down,
        "flat_count": len(rows) - up - down,
        "above_5_count": sum(1 for item in rows if item["change_pct"] > 5),
        "below_minus_5_count": sum(1 for item in rows if item["change_pct"] < -5),
        "touched_limit_up_count": len(touched), "sealed_limit_up_count": len(sealed),
        "broken_limit_up_count": len(broken), "sealed_limit_down_count": sum(1 for item in rows if item["down_sealed"]),
        "max_height": max(ladder, default=0),
        "seal_rate": _rate(len(sealed), len(touched)), "break_rate": _rate(len(broken), len(touched)),
        "promotion_rate": _rate(promoted, len(eligible)), "promoted_count": promoted,
        "median_change_pct": round(median_change, 6) if median_change is not None else None,
        "ladder": [{"height": height, "count": count} for height, count in sorted(ladder.items(), reverse=True)],
        "source_confidence": "derived_1m",
    }


def _industry_snapshots(rows: list[dict[str, Any]], minute_at: datetime, market: dict[str, Any], memberships: tuple[str, ...]) -> list[dict[str, Any]]:
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    names: dict[tuple[str, str], str] = {}
    for item in rows:
        for membership in item["memberships"]:
            source = str(membership.get("source") or "").lower()
            if source not in memberships:
                continue
            code = str(membership.get("code") or "")
            if code:
                key = (source, code)
                grouped[key].append(item)
                names.setdefault(key, str(membership.get("name") or code))
    output = []
    for (membership, code), members in grouped.items():
        name = names[(membership, code)]
        value = _snapshot(members, minute_at)
        average = sum(item["change_pct"] for item in members) / max(1, len(members))
        output.append({
            **value, "membership": membership, "industry_code": code, "industry_name": name,
            "stock_count": len(members), "relative_change_pct": round(average - _market_average(rows), 6),
            "core_pool": _stock_payloads(sorted(members, key=lambda item: (item["sealed"], item["change_pct"]), reverse=True)[:12]),
        })
    return output


def _pool_indexes(rows: list[dict[str, Any]], minute_at: datetime, memberships: tuple[str, ...]) -> list[dict[str, Any]]:
    pools = {
        "sealed": [item for item in rows if item["sealed"]],
        "broken": [item for item in rows if item["touched"] and not item["sealed"]],
        "limit_down": [item for item in rows if item["down_sealed"]],
        "ladder": [item for item in rows if item["sealed"] and item["height"] > 0],
        "yesterday_limit_up_feedback": [item for item in rows if item.get("previous_limit_status") == "limit_up"],
        "yesterday_limit_down_feedback": [item for item in rows if item.get("previous_limit_status") == "limit_down"],
    }
    output = []
    for pool_type, members in pools.items():
        output.append({"minute_at": minute_at, "pool_type": pool_type, "membership": "union", "industry_code": "", "stocks": _stock_payloads(members)})
    # The lower replay slot needs the selected industry's own pools. They are
    # materialized here rather than reconstructed from the global pool by the
    # UI, preserving the frozen THS / TDX / union membership at that date.
    scoped: dict[tuple[str, str], dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
    for row in rows:
        row_pool_types: list[str] = []
        if row["sealed"]:
            row_pool_types.append("sealed")
            if row["height"] > 0:
                row_pool_types.append("ladder")
        if row["touched"] and not row["sealed"]:
            row_pool_types.append("broken")
        if row["down_sealed"]:
            row_pool_types.append("limit_down")
        if row.get("previous_limit_status") == "limit_up":
            row_pool_types.append("yesterday_limit_up_feedback")
        if row.get("previous_limit_status") == "limit_down":
            row_pool_types.append("yesterday_limit_down_feedback")
        if not row_pool_types:
            continue
        seen: set[tuple[str, str]] = set()
        for relation in row.get("memberships") or []:
            membership = str(relation.get("source") or "").lower()
            industry_code = str(relation.get("code") or "")
            key = (membership, industry_code)
            if membership not in memberships or not industry_code or key in seen:
                continue
            seen.add(key)
            for pool_type in row_pool_types:
                scoped[key][pool_type].append(row)
    for (membership, industry_code), scoped_pools in sorted(scoped.items()):
        for pool_type, members in scoped_pools.items():
            if not members:
                continue
            output.append({
                "minute_at": minute_at,
                "pool_type": pool_type,
                "membership": membership,
                "industry_code": industry_code,
                "stocks": _stock_payloads(members),
            })
    return output


def _stock_payloads(rows: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    output = []
    for item in sorted(rows, key=lambda item: (item.get("height", 0), item["change_pct"]), reverse=True):
        relations = item.get("memberships") or []
        primary = next((relation for relation in relations if relation.get("source") == "tdx"), None)
        primary = primary or next((relation for relation in relations if relation.get("source") == "ths"), None)
        output.append({
            "code": item["code"], "name": item.get("name"), "changePct": round(item["change_pct"], 4),
            "height": item.get("height", 0), "sealed": item.get("sealed", False),
            "industry": primary.get("name") if primary else None,
            "firstSealAt": item.get("first_seal_at"), "openCount": item.get("open_count", 0),
        })
    return output


def _daily_stock_summaries(
    projections: list[MinuteProjection], contexts: dict[str, dict[str, Any]], events: Iterable[dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    """Build compact process facts from historical projections, never from later bars."""
    values: dict[str, dict[str, Any]] = {}
    was_sealed: dict[str, bool] = {}
    has_opened_since_seal: set[str] = set()
    for projection in projections:
        minute = projection.minute_at
        # Per-industry pool rows are also present in the projection. Process
        # facts must be derived from the one global row for each pool type.
        pools = {
            pool["pool_type"]: pool.get("stocks") or []
            for pool in projection.pools
            if pool.get("membership") == "union" and not pool.get("industry_code")
        }
        active: dict[str, bool] = {}
        for item in pools.get("sealed", []):
            code = item["code"]
            active[code] = True
            summary = values.setdefault(code, _summary_seed(item, contexts.get(code, {})))
            summary.update({key: value for key, value in item.items() if key in {"name", "changePct", "height", "sealed"}})
            if not summary.get("firstSealAt"):
                summary["firstSealAt"] = minute.isoformat()
            if was_sealed.get(code) is False and code in has_opened_since_seal:
                summary["resealCount"] += 1
        tracked_codes = set(was_sealed) | set(active)
        for code in tracked_codes:
            now_sealed = active.get(code, False)
            if was_sealed.get(code, False) and not now_sealed:
                summary = values.setdefault(code, _summary_seed({"code": code}, contexts.get(code, {})))
                summary["openCount"] += 1
                has_opened_since_seal.add(code)
            was_sealed[code] = now_sealed
        for pool_type in ("broken", "limit_down", "yesterday_limit_up_feedback", "yesterday_limit_down_feedback"):
            for item in pools.get(pool_type, []):
                code = item["code"]
                summary = values.setdefault(code, _summary_seed(item, contexts.get(code, {})))
                summary.update({key: value for key, value in item.items() if key in {"changePct", "height", "sealed"}})
                if pool_type == "broken":
                    summary["wasBroken"] = True
                if pool_type == "limit_down":
                    summary["wasLimitDown"] = True
    # Indicator seconds refine the first seal/open/reseal narrative where the
    # observation pool covers the security; 1m remains the broad-market base.
    for event in events:
        code = event.get("code")
        if not code:
            continue
        summary = values.setdefault(code, _summary_seed({"code": code}, contexts.get(code, {})))
        event_type = event.get("event_type")
        when = event.get("event_at")
        if event_type == "LIMIT_UP_SEAL" and when:
            observed = str(when.isoformat())
            if not summary.get("firstSealAt") or observed < summary["firstSealAt"]:
                summary["firstSealAt"] = observed
        elif event_type == "LIMIT_UP_OPEN":
            summary["indicatorOpenCount"] += 1
        elif event_type == "LIMIT_UP_TOUCH":
            summary["touchCount"] += 1
    return values


def _summary_seed(stock: dict[str, Any], context: dict[str, Any]) -> dict[str, Any]:
    return {
        "code": stock.get("code"), "name": stock.get("name") or context.get("name"),
        "changePct": stock.get("changePct"), "height": stock.get("height", 0), "sealed": stock.get("sealed", False),
        "firstSealAt": None, "openCount": 0, "indicatorOpenCount": 0, "resealCount": 0, "touchCount": 0,
        "wasBroken": False, "wasLimitDown": False, "timeshareStatus": "ready",
    }


def _enrich_daily_stock(stock: dict[str, Any], summary: dict[str, Any]) -> dict[str, Any]:
    if not summary:
        return stock
    return {**stock, **{key: value for key, value in summary.items() if key != "code" and value is not None}}


def _process_daily_pools(summaries: dict[str, dict[str, Any]], contexts: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    resealed = [summary for summary in summaries.values() if summary.get("resealCount", 0) > 0]
    break_streak = [
        summary for code, summary in summaries.items()
        if int(contexts.get(code, {}).get("previous_streak") or 0) > 0 and not summary.get("sealed")
    ]
    # A prior limit stock can have no special intraday event but must still be
    # visible in feedback.  The final feedback pools already cover it; this
    # pool makes the explicit continuation failure set queryable.
    return [
        {"pool_type": "reseal", "membership": "union", "industry_code": "", "stocks": _sort_stock_summaries(resealed)},
        # The final-minute `broken` pool is already emitted above and is
        # enriched with these same process summaries.  Do not emit a second
        # row with the same durable (date, pool, membership, industry) key.
        {"pool_type": "break_streak", "membership": "union", "industry_code": "", "stocks": _sort_stock_summaries(break_streak)},
    ]


def _scoped_daily_pools(
    daily_pools: list[dict[str, Any]], *, contexts: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    """Project global end-of-day pools into frozen THS/TDX/union industries."""
    scoped: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    existing = {
        (str(item.get("pool_type") or ""), str(item.get("membership") or "union"), str(item.get("industry_code") or ""))
        for item in daily_pools
    }
    supported = {
        "ladder", "limit_up", "broken", "reseal", "break_streak", "limit_down",
        "yesterday_limit_up_feedback", "yesterday_limit_down_feedback",
    }
    for pool in daily_pools:
        pool_type = str(pool.get("pool_type") or "")
        if pool_type not in supported or pool.get("industry_code"):
            continue
        for stock in pool.get("stocks") or []:
            code = _code(stock.get("code"))
            if not code:
                continue
            seen: set[tuple[str, str]] = set()
            for relation in contexts.get(code, {}).get("memberships") or []:
                membership = str(relation.get("source") or "").lower()
                industry_code = str(relation.get("code") or "")
                key = (membership, industry_code)
                if membership not in {"ths", "tdx", "union"} or not industry_code or key in seen:
                    continue
                seen.add(key)
                # Keep source relations available for a card that is rendered
                # from a union pool; the primary display industry remains
                # backwards-compatible with the minute payload.
                scoped[(pool_type, membership, industry_code)].append({
                    **stock,
                    "industry": relation.get("name") or stock.get("industry"),
                    "industryCode": industry_code,
                })
    return [
        {
            "pool_type": pool_type,
            "membership": membership,
            "industry_code": industry_code,
            "stocks": _sort_stock_summaries(stocks),
        }
        for (pool_type, membership, industry_code), stocks in sorted(scoped.items())
        if stocks and (pool_type, membership, industry_code) not in existing
    ]


def _sort_stock_summaries(values: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(values, key=lambda value: (int(value.get("height") or 0), float(value.get("changePct") or -999)), reverse=True)


def _promotion_rate(pools: list[dict[str, Any]], previous_contexts: dict[str, dict[str, Any]]) -> float | None:
    ladder = next((item["stocks"] for item in pools if item["pool_type"] == "ladder"), [])
    eligible = [context for context in previous_contexts.values() if int(context.get("previous_streak") or 0) > 0 and context.get("security_type") == "STOCK"]
    if not eligible:
        return None
    promoted = sum(1 for stock in ladder if int(previous_contexts.get(stock["code"], {}).get("previous_streak") or 0) > 0)
    return _rate(promoted, len(eligible))


def _break_streak_rate(pools: list[dict[str, Any]], previous_contexts: dict[str, dict[str, Any]]) -> float | None:
    eligible = [context for context in previous_contexts.values() if int(context.get("previous_streak") or 0) >= 2 and context.get("security_type") == "STOCK"]
    if not eligible:
        return None
    sealed_codes = {item["code"] for pool in pools if pool["pool_type"] == "sealed" for item in pool["stocks"]}
    return _rate(sum(1 for context in eligible if context.get("code") not in sealed_codes), len(eligible))


def _market_average(rows: list[dict[str, Any]]) -> float:
    return sum(item["change_pct"] for item in rows) / max(1, len(rows))


def _rate(numerator: int, denominator: int) -> float | None:
    return round(numerator / denominator * 100, 6) if denominator else None


def _code(value: Any) -> str:
    digits = "".join(char for char in str(value or "") if char.isdigit())
    return digits[-6:].zfill(6) if digits else ""


def _number(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _datetime_order(value: Any) -> float:
    """Order PG timestamptz and ClickHouse naive timestamps safely.

    ClickHouse's Python driver returns local naive trading timestamps while
    PostgreSQL returns aware timestamptz values. Both represent the same
    China-market wall-clock timeline for this projection, so compare their
    calendar fields rather than letting Python reject the mixed values.
    """
    if not isinstance(value, datetime):
        return float("inf")
    local = value.astimezone(ZoneInfo("Asia/Shanghai")).replace(tzinfo=None) if value.tzinfo else value
    return float(local.timestamp())
