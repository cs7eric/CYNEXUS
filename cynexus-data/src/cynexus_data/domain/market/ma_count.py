from __future__ import annotations

from datetime import date
from typing import Any


def calculate_ma_count_summary(rows: list[dict[str, Any]], *, trade_date: date) -> dict[str, Any]:
    total = above_ma20 = above_ma60 = above_both = 0
    up_5d = new_low_60d = new_high_252d = advancing = 0
    for row in rows:
        close = _to_float(row.get("close"))
        ma20 = _to_float(row.get("ma20"))
        ma60 = _to_float(row.get("ma60"))
        if close is None or ma20 is None or ma60 is None:
            continue
        total += 1
        is_above_ma20 = close > ma20
        is_above_ma60 = close > ma60
        if is_above_ma20:
            above_ma20 += 1
        if is_above_ma60:
            above_ma60 += 1
        if is_above_ma20 and is_above_ma60:
            above_both += 1
        close_1d_ago = _to_float(row.get("close_1d_ago"))
        close_5d_ago = _to_float(row.get("close_5d_ago"))
        close_59d_ago = _to_float(row.get("close_59d_ago"))
        min_60d = _to_float(row.get("min_60d"))
        close_251d_ago = _to_float(row.get("close_251d_ago"))
        max_252d = _to_float(row.get("max_252d"))
        if close_1d_ago is not None and close > close_1d_ago:
            advancing += 1
        if close_5d_ago is not None and close > close_5d_ago:
            up_5d += 1
        if close_59d_ago is not None and min_60d is not None and close <= min_60d:
            new_low_60d += 1
        if close_251d_ago is not None and max_252d is not None and close >= max_252d:
            new_high_252d += 1

    pct_above_ma20 = _pct(above_ma20, total)
    pct_above_ma60 = _pct(above_ma60, total)
    pct_advancing = _pct(advancing, total)
    breadth_raw = round(0.40 * pct_advancing + 0.35 * pct_above_ma20 + 0.25 * pct_above_ma60, 2)
    price_strength_score = _pct(new_high_252d, total)
    return {
        "tradeDate": trade_date.isoformat(),
        "totalEligible": total,
        "aboveMa20": above_ma20,
        "aboveMa60": above_ma60,
        "aboveBoth": above_both,
        "pctAboveMa20": pct_above_ma20,
        "pctAboveMa60": pct_above_ma60,
        "pctAboveBoth": _pct(above_both, total),
        "up5dCount": up_5d,
        "pctUp5d": _pct(up_5d, total),
        "newLow60dCount": new_low_60d,
        "pctNewLow60d": _pct(new_low_60d, total),
        "newHigh252dCount": new_high_252d,
        "pctNewHigh252d": _pct(new_high_252d, total),
        "advancingCount": advancing,
        "pctAdvancing": pct_advancing,
        "breadthRaw": breadth_raw,
        "priceStrengthScore": price_strength_score,
        "breadthScore": breadth_raw,
        "source": "clickhouse.daily_qfq",
    }


def calculate_profit_effect(*, trade_date: date, up_5d_pct: float | int | str | None, new_low_60d_pct: float | int | str | None) -> dict[str, Any]:
    up = _to_float(up_5d_pct) or 0.0
    low = _to_float(new_low_60d_pct) or 0.0
    score = round(0.60 * up + 0.40 * (100 - low), 2)
    return {
        "tradeDate": trade_date.isoformat(),
        "up5dPct": up,
        "newLow60dPct": low,
        "score": score,
        "source": "postgres.msi_ma_count_daily",
    }


def _pct(num: int, den: int) -> float:
    return round(num / den * 100, 2) if den > 0 else 0.0


def _to_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None
