from __future__ import annotations

import math
import statistics
from datetime import date
from typing import Any


def calculate_turnover_activity(rows: list[dict[str, Any]], *, trade_date: date, window: int = 20, score: float | None = None) -> dict[str, Any] | None:
    ordered = _rows_by_date(rows)
    if len(ordered) < 2:
        return None
    window = max(1, int(window))
    today = ordered[-1]
    prev = ordered[:-1][-window:]
    total_amount = _to_float(today.get("total_amount") or today.get("totalAmount") or today.get("total_amount_yi"))
    sh_amount = _to_float(today.get("sh_amount") or today.get("shAmount"))
    sz_amount = _to_float(today.get("sz_amount") or today.get("szAmount"))
    sample = [_to_float(r.get("total_amount") or r.get("totalAmount") or r.get("total_amount_yi")) for r in prev]
    sample = [v for v in sample if v is not None]
    if total_amount is None or not sample:
        return None
    avg_amount = sum(sample) / len(sample)
    ratio = total_amount / avg_amount if avg_amount > 0 else 0.0
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "totalAmount": round(total_amount, 2),
        "shAmount": round(sh_amount, 2) if sh_amount is not None else None,
        "szAmount": round(sz_amount, 2) if sz_amount is not None else None,
        "avg20dAmount": round(avg_amount, 2),
        "ratio": round(ratio, 4),
        "score": _score_or_neutral(score),
        "sampleCount": len(sample),
        "source": "clickhouse.daily_raw.999999+399001",
    }


def calculate_volatility_sentiment(
    rows: list[dict[str, Any]],
    *,
    trade_date: date,
    underlying_code: str = "sh000300",
    underlying_name: str = "沪深300",
    window: int = 20,
    lookback: int = 252,
) -> dict[str, Any] | None:
    ordered = _rows_by_date(rows)
    window = max(2, min(int(window), 60))
    lookback = max(window + 1, min(int(lookback), 1000))
    if len(ordered) < window + 2:
        return None
    closes = [_to_float(r.get("close")) for r in ordered]
    if any(v is None for v in closes):
        return None
    close_values = [float(v) for v in closes if v is not None]
    returns = [(close_values[i] - close_values[i - 1]) / close_values[i - 1] if close_values[i - 1] > 0 else 0.0 for i in range(1, len(close_values))]
    vol_series: list[float] = []
    for index in range(window, len(close_values)):
        segment = returns[index - window:index]
        if len(segment) >= 2:
            vol_series.append(statistics.stdev(segment) * math.sqrt(252) * 100)
    if not vol_series:
        return None
    current_vol = vol_series[-1]
    sample = vol_series[-lookback - 1:-1] if len(vol_series) >= lookback + 1 else vol_series[:-1]
    percentile = _percentile_rank(current_vol, sample)
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "underlyingCode": underlying_code,
        "underlyingName": underlying_name,
        "close": round(close_values[-1], 4),
        "dailyReturnPct": round(returns[-1] * 100, 4) if returns else None,
        "realizedVol20d": round(current_vol, 2),
        "volWindowDays": window,
        "volLookbackDays": lookback,
        "percentile1y": round(percentile, 4),
        "sentimentScore": round((1.0 - percentile) * 100.0, 2),
        "sampleCount": len(sample),
        "source": "clickhouse.index_daily_raw",
    }


def calculate_risk_appetite(
    *,
    trade_date: date,
    hs300_rows: list[dict[str, Any]],
    treasury_511010_rows: list[dict[str, Any]],
    treasury_511090_rows: list[dict[str, Any]],
    window: int = 20,
    score: float | None = None,
) -> dict[str, Any] | None:
    window = max(2, min(int(window), 250))
    hs = _return_payload(hs300_rows, window=window)
    t1 = _return_payload(treasury_511010_rows, window=window)
    t2 = _return_payload(treasury_511090_rows, window=window)
    if hs.get("returnPct") is None:
        return None
    weighted = _weighted_return(t1.get("returnPct"), t2.get("returnPct"))
    spread_weighted = _spread(hs.get("returnPct"), weighted)
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "windowDays": window,
        "hs300": hs,
        "treasury": {
            "511010": {**t1, "weight": 0.5},
            "511090": {**t2, "weight": 0.5},
            "weighted": {"returnPct": weighted},
        },
        "spread": {
            "511010": _spread(hs.get("returnPct"), t1.get("returnPct")),
            "511090": _spread(hs.get("returnPct"), t2.get("returnPct")),
            "weighted": spread_weighted,
        },
        "score": _score_or_neutral(score),
        "rawValue": spread_weighted,
        "source": "clickhouse.daily_qfq",
    }


def calculate_style_risk(
    *,
    trade_date: date,
    hs300_rows: list[dict[str, Any]],
    csi1000_rows: list[dict[str, Any]],
    window: int = 5,
    score: float | None = None,
) -> dict[str, Any] | None:
    window = max(2, min(int(window), 250))
    hs = _return_payload(hs300_rows, window=window)
    csi = _return_payload(csi1000_rows, window=window)
    hs_ret = hs.get("returnPct")
    csi_ret = csi.get("returnPct")
    if hs_ret is None or csi_ret is None:
        return None
    spread = round(float(csi_ret) - float(hs_ret), 4)
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "windowDays": window,
        "hs300": {"name": "沪深300", "code": "sh000300", **hs},
        "csi1000": {"name": "中证1000", "code": "sh000852", **csi},
        "spread": spread,
        "score": _score_or_neutral(score),
        "rawValue": spread,
        "source": "clickhouse.daily_qfq",
    }


def calculate_limit_emotion(
    *,
    trade_date: date,
    today_rows: list[dict[str, Any]],
    prev_rows: list[dict[str, Any]] | None = None,
    prev_trade_date: date | None = None,
    score_overrides: dict[str, float] | None = None,
) -> dict[str, Any]:
    prev_rows = prev_rows or []
    up_count = sum(1 for row in today_rows if _truthy(row.get("isLimitUp") or row.get("is_limit_up")))
    down_count = sum(1 for row in today_rows if _truthy(row.get("isLimitDown") or row.get("is_limit_down")))
    touched_count = sum(1 for row in today_rows if _truthy(row.get("isTouchedLimitUp") or row.get("is_touched_limit_up") or row.get("is_touched")))
    broken_count = sum(1 for row in today_rows if _truthy(row.get("isBrokenLimitUp") or row.get("is_broken_limit_up") or row.get("is_broken")))
    break_rate = round(broken_count / touched_count, 4) if touched_count > 0 else None
    up_down_ratio = round(up_count / max(down_count, 1), 4)
    today_by_code = {str(row.get("code")): row for row in today_rows if row.get("code") is not None}
    prev_limit_codes = {str(row.get("code")) for row in prev_rows if _truthy(row.get("isLimitUp") or row.get("is_limit_up")) and row.get("code") is not None}
    returns = [_to_float(today_by_code[code].get("changePct") or today_by_code[code].get("change_pct")) for code in prev_limit_codes if code in today_by_code]
    returns = [v for v in returns if v is not None]
    yest_avg_return = round(sum(returns) / len(returns), 4) if returns else None
    streak_summary = summarize_limit_streaks(today_rows, trade_date=trade_date)
    overrides = score_overrides or {}
    up_down_score = _score_or(overrides.get("upDownScore"), _score_up_down(up_down_ratio))
    break_score = _score_or(overrides.get("breakBoardScore"), _score_break_board(break_rate))
    yesterday_score = _score_or(overrides.get("yesterdayReturnScore"), _score_yesterday_return(yest_avg_return))
    composite_raw = round(0.4 * up_down_score + 0.3 * break_score + 0.3 * yesterday_score, 2)
    composite = _score_or(overrides.get("compositeScore"), composite_raw)
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "prevTradeDate": prev_trade_date.isoformat() if prev_trade_date else None,
        "limitUpCount": up_count,
        "limitDownCount": down_count,
        "touchedCount": touched_count,
        "brokenCount": broken_count,
        "breakBoardRate": break_rate,
        "limitUpDownRatio": up_down_ratio,
        "yesterdayLimitUpCount": len(prev_limit_codes),
        "yesterdayLimitUpAvgReturn": yest_avg_return,
        "streakSummary": streak_summary,
        "components": {
            "upDownScore": up_down_score,
            "breakBoardScore": break_score,
            "yesterdayReturnScore": yesterday_score,
        },
        "compositeScore": composite,
        "level": _level_for(composite),
        "source": "clickhouse.daily_raw",
    }


def extract_limit_stock_snapshots(rows: list[dict[str, Any]], *, trade_date: date) -> list[dict[str, Any]]:
    snapshots: list[dict[str, Any]] = []
    for row in rows:
        code = str(row.get("code") or "").zfill(6)
        if not code or code == "000000":
            continue
        category: str | None = None
        streak: int | None = None
        if _truthy(row.get("isLimitUp") or row.get("is_limit_up")):
            category = "limit_up"
            streak = int(row.get("limitUpStreak") or row.get("limit_up_streak") or 0)
        elif _truthy(row.get("isLimitDown") or row.get("is_limit_down")):
            category = "limit_down"
            streak = 0
        elif _truthy(row.get("isBrokenLimitUp") or row.get("is_broken_limit_up") or row.get("is_broken")):
            category = "broken"
            streak = int(row.get("previousLimitUpStreak") or row.get("previous_limit_up_streak") or 0)
        if category is None:
            continue
        snapshots.append(
            {
                "tradeDate": _ensure_date(trade_date).isoformat(),
                "code": code,
                "name": row.get("name"),
                "category": category,
                "streak": streak,
                "changePct": _to_float(row.get("changePct") or row.get("change_pct")),
                "limitUpPrice": _to_float(row.get("limitUpPrice") or row.get("limit_up_price")),
                "limitDownPrice": _to_float(row.get("limitDownPrice") or row.get("limit_down_price")),
                "close": _to_float(row.get("close")),
                "payload": dict(row),
            }
        )
    return snapshots


def build_limit_snapshot_rows(
    rows: list[dict[str, Any]],
    *,
    stock_meta: dict[str, dict[str, Any]] | None = None,
    history_rows: list[dict[str, Any]] | None = None,
    tolerance: float = 0.0001,
    high_tolerance: float = 0.0005,
) -> list[dict[str, Any]]:
    meta_by_code = stock_meta or {}
    prev_streak_by_code = _previous_streaks(history_rows or [], meta_by_code=meta_by_code, tolerance=tolerance)
    out: list[dict[str, Any]] = []
    for row in rows:
        code = str(row.get("code") or row.get("symbol") or "").zfill(6)
        if not code or code == "000000":
            continue
        close = _to_float(row.get("close"))
        high = _to_float(row.get("high"))
        pre_close = _to_float(row.get("preClose") or row.get("pre_close"))
        if close is None or high is None or pre_close is None or pre_close <= 0:
            continue
        threshold = _limit_threshold(code, meta_by_code.get(code))
        limit_up_price = pre_close * (1.0 + threshold)
        limit_down_price = pre_close * (1.0 - threshold)
        is_limit_up = close >= limit_up_price * (1.0 - tolerance)
        is_limit_down = close <= limit_down_price * (1.0 + tolerance)
        is_touched = high >= limit_up_price * (1.0 - high_tolerance)
        prev_streak = prev_streak_by_code.get(code, 0)
        current_streak = prev_streak + 1 if is_limit_up else 0
        out.append(
            {
                "code": code,
                "name": (meta_by_code.get(code) or {}).get("name"),
                "open": _to_float(row.get("open")),
                "high": high,
                "low": _to_float(row.get("low")),
                "close": close,
                "latestPrice": close,
                "preClose": pre_close,
                "limitUpPrice": round(limit_up_price, 4),
                "limitDownPrice": round(limit_down_price, 4),
                "changePct": round((close - pre_close) / pre_close * 100.0, 4),
                "isLimitUp": is_limit_up,
                "isLimitDown": is_limit_down,
                "isTouchedLimitUp": is_touched,
                "isBrokenLimitUp": is_touched and not is_limit_up,
                "previousLimitUpStreak": prev_streak,
                "limitUpStreak": current_streak,
                "isPromoted": is_limit_up and prev_streak > 0,
                "isBrokenStreak": (not is_limit_up) and prev_streak > 0,
                "limitThreshold": threshold,
                "thresholdSource": "stock_meta" if code in meta_by_code else "code_rule",
            }
        )
    return out


def summarize_limit_streaks(rows: list[dict[str, Any]], *, trade_date: date) -> dict[str, Any]:
    distribution: dict[int, list[dict[str, Any]]] = {}
    broken_rows: list[dict[str, Any]] = []
    promoted_rows: list[dict[str, Any]] = []
    prev_counts: dict[int, int] = {}
    promoted_counts: dict[int, int] = {}
    for row in rows:
        code = str(row.get("code") or "")
        streak = int(row.get("limitUpStreak") or 0)
        prev_streak = int(row.get("previousLimitUpStreak") or 0)
        if streak > 0:
            distribution.setdefault(streak, []).append({"code": code})
        if _truthy(row.get("isBrokenStreak")):
            broken_rows.append(row)
        if prev_streak > 0:
            prev_counts[prev_streak] = prev_counts.get(prev_streak, 0) + 1
        if _truthy(row.get("isPromoted")):
            promoted_rows.append(row)
            promoted_counts[prev_streak] = promoted_counts.get(prev_streak, 0) + 1
    max_height = max(distribution.keys(), default=0)
    levels = []
    for prev_streak in sorted(prev_counts):
        total = prev_counts[prev_streak]
        promoted = promoted_counts.get(prev_streak, 0)
        levels.append(
            {
                "from": prev_streak,
                "to": prev_streak + 1,
                "yesterdayCount": total,
                "todayPromotedCount": promoted,
                "rate": round(promoted / total * 100, 2) if total else None,
            }
        )
    total_prev = sum(prev_counts.values())
    total_promoted = sum(promoted_counts.values())
    return {
        "tradeDate": _ensure_date(trade_date).isoformat(),
        "maxHeight": max_height,
        "leaders": distribution.get(max_height, []) if max_height > 0 else [],
        "distribution": [{"streak": key, "count": len(value), "stocks": value} for key, value in sorted(distribution.items(), reverse=True)],
        "broken": {
            "count": len(broken_rows),
            "highStreakBrokenCount": sum(1 for row in broken_rows if int(row.get("previousLimitUpStreak") or 0) >= 3),
            "stocks": [{"code": str(row.get("code") or ""), "previousStreak": int(row.get("previousLimitUpStreak") or 0), "changePct": _to_float(row.get("changePct"))} for row in broken_rows],
        },
        "promoted": {
            "overallRate": round(total_promoted / total_prev * 100, 2) if total_prev else None,
            "levels": levels,
        },
    }


def _return_payload(rows: list[dict[str, Any]], *, window: int) -> dict[str, Any]:
    ordered = _rows_by_date(rows)
    if len(ordered) < 2:
        return {"close": None, "currentDate": None, "baseClose": None, "baseDate": None, "returnPct": None, "barsUsed": len(ordered)}
    recent = ordered[-window:] if len(ordered) >= window else ordered
    current = _to_float(recent[-1].get("close"))
    base = _to_float(recent[0].get("close"))
    if current is None or base is None or base <= 0:
        ret = None
    else:
        ret = round((current - base) / base * 100, 4)
    return {
        "close": round(current, 4) if current is not None else None,
        "currentDate": _date_key(recent[-1]).isoformat() if _date_key(recent[-1]) else None,
        "baseClose": round(base, 4) if base is not None else None,
        "baseDate": _date_key(recent[0]).isoformat() if _date_key(recent[0]) else None,
        "returnPct": ret,
        "barsUsed": len(recent),
    }


def _rows_by_date(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(rows, key=lambda row: _date_key(row) or date.min)


def _previous_streaks(rows: list[dict[str, Any]], *, meta_by_code: dict[str, dict[str, Any]], tolerance: float) -> dict[str, int]:
    grouped: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        code = str(row.get("code") or row.get("symbol") or "").zfill(6)
        if not code or code == "000000":
            continue
        grouped.setdefault(code, []).append(row)
    out: dict[str, int] = {}
    for code, code_rows in grouped.items():
        streak = 0
        threshold = _limit_threshold(code, meta_by_code.get(code))
        for row in reversed(_rows_by_date(code_rows)):
            close = _to_float(row.get("close"))
            pre_close = _to_float(row.get("preClose") or row.get("pre_close"))
            if close is None or pre_close is None or pre_close <= 0:
                break
            if close >= pre_close * (1.0 + threshold) * (1.0 - tolerance):
                streak += 1
                continue
            break
        out[code] = streak
    return out


def _date_key(row: dict[str, Any]) -> date | None:
    value = row.get("trade_date") or row.get("tradeDate") or row.get("date")
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _ensure_date(value: Any) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _to_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _weighted_return(a: Any, b: Any) -> float | None:
    a_float = _to_float(a)
    b_float = _to_float(b)
    if a_float is not None and b_float is not None:
        return round(a_float * 0.5 + b_float * 0.5, 4)
    if a_float is not None:
        return a_float
    return b_float


def _spread(a: Any, b: Any) -> float | None:
    a_float = _to_float(a)
    b_float = _to_float(b)
    return round(a_float - b_float, 4) if a_float is not None and b_float is not None else None


def _percentile_rank(current: float, history: list[float]) -> float:
    if not history:
        return 0.5
    return sum(1 for value in history if value <= current) / len(history)


def _score_up_down(ratio: float | None) -> float:
    if ratio is None or ratio <= 0:
        return 0.0
    return round(max(0.0, min(100.0, 50.0 + 25.0 * math.log2(ratio))), 2)


def _score_break_board(rate: float | None) -> float:
    if rate is None:
        return 50.0
    return round(max(0.0, min(100.0, 100.0 - 100.0 * rate)), 2)


def _score_yesterday_return(avg_return_pct: float | None) -> float:
    if avg_return_pct is None:
        return 50.0
    return round(max(0.0, min(100.0, 50.0 + 10.0 * avg_return_pct)), 2)


def _level_for(score: float) -> str:
    if score >= 80:
        return "hot"
    if score >= 60:
        return "active"
    if score >= 40:
        return "normal"
    if score >= 20:
        return "weak"
    return "ice"


def _score_or(value: Any, fallback: float) -> float:
    parsed = _to_float(value)
    return round(max(0.0, min(100.0, parsed)), 2) if parsed is not None else fallback


def _score_or_neutral(value: Any) -> float:
    return _score_or(value, 50.0)


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _limit_threshold(code: str, meta: dict[str, Any] | None) -> float:
    threshold = _to_float((meta or {}).get("limit_up_threshold"))
    is_st = _truthy((meta or {}).get("is_st")) if meta else False
    if is_st and threshold is None:
        return 0.05
    if threshold is not None and threshold > 0:
        return threshold
    if code.startswith(("300", "301", "688")):
        return 0.20
    if code.startswith(("4", "8", "9")):
        return 0.30
    return 0.10
