from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from math import sqrt
from statistics import median
from typing import Any


WINDOWS = [5, 10, 15, 20, 30, 60, 100, 120, 250]
FORWARD_WINDOWS = [5, 10, 15, 20, 30, 60, 100, 120, 250]
WINDOW_CONFIG = {
    5: {"slope_lookback": 3, "vol_window": 5, "ma_periods": [5]},
    10: {"slope_lookback": 5, "vol_window": 10, "ma_periods": [5, 10]},
    15: {"slope_lookback": 5, "vol_window": 15, "ma_periods": [10]},
    20: {"slope_lookback": 5, "vol_window": 20, "ma_periods": [20]},
    30: {"slope_lookback": 10, "vol_window": 30, "ma_periods": [20, 30]},
    60: {"slope_lookback": 20, "vol_window": 60, "ma_periods": [20, 60]},
    100: {"slope_lookback": 30, "vol_window": 100, "ma_periods": [60]},
    120: {"slope_lookback": 30, "vol_window": 120, "ma_periods": [60, 120]},
    250: {"slope_lookback": 60, "vol_window": 250, "ma_periods": [120, 250]},
}
STYLE_MAPPING = [
    {"style": "large_cap", "sources": ["sse50", "hs300"]},
    {"style": "mid_cap", "sources": ["csi500"]},
    {"style": "small_cap", "sources": ["csi1000"]},
    {"style": "micro_cap", "sources": ["csi2000"]},
    {"style": "growth", "sources": ["chinext", "star50"]},
]


@dataclass(frozen=True)
class PriceLevel:
    price: float
    type: str
    source: str
    strength: int
    label: str
    distance_pct: float

    def to_dict(self) -> dict[str, Any]:
        return {
            "price": round(self.price, 2),
            "type": self.type,
            "source": self.source,
            "strength": self.strength,
            "label": self.label,
            "distancePct": round(self.distance_pct, 4),
        }


def safe_div(a: float | None, b: float | None) -> float | None:
    if a is None or b in (None, 0):
        return None
    return a / b


def moving_average(values: list[float], n: int) -> list[float | None]:
    if n <= 0:
        return [None for _ in values]
    result: list[float | None] = []
    acc = 0.0
    for index, value in enumerate(values):
        acc += value
        if index >= n:
            acc -= values[index - n]
        result.append(acc / n if index >= n - 1 else None)
    return result


def calc_daily_returns(closes: list[float]) -> list[float | None]:
    returns: list[float | None] = [None]
    for index in range(1, len(closes)):
        prev = closes[index - 1]
        returns.append((closes[index] / prev - 1) if prev else None)
    return returns


def calc_return(bars: list[dict[str, Any]], window: int) -> float | None:
    if len(bars) <= window:
        return None
    current = _float(bars[-1]["close"])
    previous = _float(bars[-1 - window]["close"])
    if not previous:
        return None
    return current / previous - 1


def calc_volatility(returns: list[float | None], window: int) -> list[float | None]:
    result: list[float | None] = []
    for index in range(len(returns)):
        if index < window:
            result.append(None)
            continue
        sample = [value for value in returns[index - window + 1 : index + 1] if value is not None]
        if len(sample) < 2:
            result.append(None)
            continue
        mean = sum(sample) / len(sample)
        variance = sum((value - mean) ** 2 for value in sample) / len(sample)
        result.append(sqrt(variance))
    return result


def latest_window_metrics(bars: list[dict[str, Any]]) -> list[dict[str, Any]]:
    if not bars:
        return []
    closes = [_float(item["close"]) for item in bars]
    highs = [_float(item["high"]) for item in bars]
    lows = [_float(item["low"]) for item in bars]
    volumes = [_float(item.get("volume") or 0) for item in bars]
    amounts = [_float(item.get("turnover") or item.get("amount") or 0) for item in bars]
    returns = calc_daily_returns(closes)
    last_idx = len(bars) - 1
    close = closes[last_idx]
    precomputed_ma: dict[int, list[float | None]] = {}
    precomputed_vol: dict[int, list[float | None]] = {}
    result: list[dict[str, Any]] = []

    for window in WINDOWS:
        if len(bars) <= window:
            continue
        start = len(bars) - window - 1
        window_high = max(highs[start + 1 :])
        window_low = min(lows[start + 1 :])
        prev_close = closes[start]
        range_width = window_high - window_low
        up_days = [value for value in returns[start + 1 :] if value is not None and value > 0]
        all_days = [value for value in returns[start + 1 :] if value is not None]
        volume_avg = sum(volumes[start + 1 :]) / window
        amount_avg = sum(amounts[start + 1 :]) / window if any(amounts[start + 1 :]) else 0

        cfg = WINDOW_CONFIG.get(window, {})
        slope_lookback = int(cfg.get("slope_lookback", 5))
        vol_window_cfg = int(cfg.get("vol_window", window))
        ma_periods = list(cfg.get("ma_periods", [20, 60]))

        window_vol = precomputed_vol.get(vol_window_cfg)
        if window_vol is None:
            window_vol = calc_volatility(returns, vol_window_cfg)
            precomputed_vol[vol_window_cfg] = window_vol
        volatility_val = window_vol[last_idx] if last_idx < len(window_vol) else None

        ma_slopes: dict[int, float | None] = {}
        for ma_period in ma_periods:
            ma_vals = _ma(precomputed_ma, closes, int(ma_period))
            ma_last = ma_vals[last_idx]
            slope = None
            if ma_last and last_idx >= slope_lookback:
                ma_prev = ma_vals[last_idx - slope_lookback]
                if ma_prev:
                    slope = round(ma_last / ma_prev - 1, 4)
            ma_slopes[int(ma_period)] = slope

        ma20_last = _ma(precomputed_ma, closes, 20)[last_idx]
        ma60_last = _ma(precomputed_ma, closes, 60)[last_idx]
        result.append(
            {
                "window": window,
                "returnN": round(close / prev_close - 1, 4) if prev_close else None,
                "highN": round(window_high, 2),
                "lowN": round(window_low, 2),
                "rangePosition": round((close - window_low) / range_width, 4) if range_width > 0 else None,
                "drawdownFromHigh": round(close / window_high - 1, 4) if window_high else None,
                "reboundFromLow": round(close / window_low - 1, 4) if window_low else None,
                "volatility": round(volatility_val, 4) if volatility_val is not None else None,
                "upDaysRatio": round(len(up_days) / len(all_days), 4) if all_days else None,
                "volumeRatio": round(volumes[last_idx] / volume_avg, 4) if volume_avg else None,
                "amountRatio": round(amounts[last_idx] / amount_avg, 4) if amount_avg else None,
                "closeAboveMa20": bool(ma20_last is not None and close > ma20_last),
                "closeAboveMa60": bool(ma60_last is not None and close > ma60_last),
                "ma20Slope": ma_slopes.get(20),
                "ma60Slope": ma_slopes.get(60),
            }
        )
    return result


def build_support_resistance(
    bars: list[dict[str, Any]],
    window_metrics: list[dict[str, Any]] | None = None,
    *,
    latest_close: float | None = None,
    ma20: float | None = None,
    ma60: float | None = None,
    ma120: float | None = None,
    ma250: float | None = None,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not bars:
        return [], []
    close = _float(latest_close if latest_close is not None else bars[-1]["close"])
    metrics = window_metrics if window_metrics is not None else latest_window_metrics(bars)
    supports: list[PriceLevel] = []
    resistances: list[PriceLevel] = []

    for label, value, source, strength in [
        ("MA20", ma20, "ma", 70),
        ("MA60", ma60, "ma", 80),
        ("MA120", ma120, "ma", 85),
        ("MA250", ma250, "ma", 90),
    ]:
        if not value:
            continue
        level_type = "support" if value <= close else "resistance"
        level = PriceLevel(_float(value), level_type, source, strength, label, _float(value) / close - 1)
        (supports if level_type == "support" else resistances).append(level)

    for item in metrics:
        window = int(item["window"])
        high_n = item.get("highN")
        low_n = item.get("lowN")
        if high_n:
            resistances.append(PriceLevel(_float(high_n), "resistance", "rangeHighLow", 55 + min(window // 5, 35), f"{window}d high", _float(high_n) / close - 1))
        if low_n:
            supports.append(PriceLevel(_float(low_n), "support", "rangeHighLow", 55 + min(window // 5, 35), f"{window}d low", _float(low_n) / close - 1))

    gap_supports, gap_resistances = _build_gap_levels(bars, close)
    swing_supports, swing_resistances = _build_swing_cluster_levels(bars, close)
    volume_supports, volume_resistances = _build_volume_node_levels(bars, close)
    supports.extend(gap_supports + swing_supports + volume_supports)
    resistances.extend(gap_resistances + swing_resistances + volume_resistances)
    return _merge_levels(supports, close, "support"), _merge_levels(resistances, close, "resistance")


def build_style_overview(
    index_bars_map: dict[str, list[dict[str, Any]]],
    benchmark_bars: list[dict[str, Any]],
    *,
    style_mapping: list[dict[str, Any]] | None = None,
) -> list[dict[str, Any]]:
    mapping = style_mapping or STYLE_MAPPING
    result: list[dict[str, Any]] = []
    for row in mapping:
        source_names = [str(item) for item in row.get("sources") or []]
        rel5 = _style_relative_return(index_bars_map, source_names, benchmark_bars, 5)
        rel20 = _style_relative_return(index_bars_map, source_names, benchmark_bars, 20)
        rel60 = _style_relative_return(index_bars_map, source_names, benchmark_bars, 60)
        values = [value for value in [rel5, rel20, rel60] if value is not None]
        avg_score = sum(values) / len(values) if values else 0
        result.append(
            {
                "style": str(row.get("style") or ""),
                "source": source_names[0] if source_names else None,
                "relativeReturn5": rel5,
                "relativeReturn20": rel20,
                "relativeReturn60": rel60,
                "state": _relative_strength_state(avg_score),
            }
        )
    return result


def dominant_style_from_rows(rows: list[dict[str, Any]], *, neutral_label: str = "neutral") -> str:
    ranked: list[tuple[float, str]] = []
    for item in rows:
        values = [value for value in [item.get("relativeReturn5"), item.get("relativeReturn20"), item.get("relativeReturn60")] if value is not None]
        if values:
            ranked.append((sum(_float(value) for value in values) / len(values), str(item.get("style") or "")))
    if not ranked:
        return neutral_label
    ranked.sort(key=lambda item: item[0], reverse=True)
    return ranked[0][1]


def style_similarity(candidate_style: str, current_style: str, *, neutral_label: str = "neutral") -> float:
    if candidate_style == current_style:
        return 0.0
    if candidate_style == neutral_label or current_style == neutral_label:
        return 4.0
    return 8.0


def build_industry_strength_overview(
    industry_rows: list[dict[str, Any]],
    benchmark_bars: list[dict[str, Any]],
    index_bars_map: dict[str, list[dict[str, Any]]],
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    seen_symbols: set[str] = set()
    for industry in industry_rows:
        symbol = str(industry.get("symbol") or industry.get("sectorIndexSymbol") or "").strip()
        index_name = str(industry.get("indexName") or industry.get("sectorIndexName") or symbol).strip() or symbol
        name = str(industry.get("name") or industry.get("industryName") or industry.get("industryQuery") or index_name).strip()
        if not symbol or symbol in seen_symbols:
            continue
        bars = index_bars_map.get(index_name) or index_bars_map.get(symbol)
        if not bars or len(bars) < 61 or len(benchmark_bars) < 61:
            continue
        idx_returns = [calc_return(bars, window) for window in [5, 20, 60]]
        benchmark_returns = [calc_return(benchmark_bars, window) for window in [5, 20, 60]]
        if any(value is None for value in idx_returns + benchmark_returns):
            continue
        rr5, rr20, rr60 = [round(_float(idx_returns[i]) - _float(benchmark_returns[i]), 4) for i in range(3)]
        avg_score = (rr5 + rr20 + rr60) / 3
        score = rr5 * 60 + rr20 * 100 + rr60 * 80
        result.append(
            {
                "name": name,
                "indexName": index_name,
                "symbol": symbol,
                "industryQuery": str(industry.get("industryQuery") or name),
                "relativeReturn5": rr5,
                "relativeReturn20": rr20,
                "relativeReturn60": rr60,
                "state": _relative_strength_state(avg_score),
                "score": round(score, 2),
            }
        )
        seen_symbols.add(symbol)
    result.sort(key=lambda item: item["score"], reverse=True)
    return result


def score_breadth_components(item: dict[str, Any]) -> dict[str, Any]:
    up_count = _float(item.get("upCount") or item.get("rising_count") or item.get("risingCount") or 0)
    down_count = _float(item.get("downCount") or item.get("falling_count") or item.get("fallingCount") or 0)
    limit_up = _float(item.get("limitUpCount") or item.get("limit_up_count") or 0)
    limit_down = _float(item.get("limitDownCount") or item.get("limit_down_count") or 0)
    total = _float(item.get("totalCount") or item.get("stockCount") or item.get("stock_count") or (up_count + down_count) or 1)
    break_rate = _float(item.get("breakRate") or item.get("break_rate") or 0)
    max_lianban = _float(item.get("maxLianBan") or item.get("max_lianban") or 0)
    yesterday_return = _float(item.get("yesterdayLimitUpReturn") or item.get("yesterday_limit_up_return") or 0)
    down_over_5 = _float(item.get("downOver5Count") or item.get("down_over_5_count") or 0)
    new_high_20 = _float(item.get("new20HighCount") or item.get("new_20_high_count") or 0)
    new_low_20 = _float(item.get("new20LowCount") or item.get("new_20_low_count") or 0)
    breadth = (up_count - down_count) / total if total else 0
    limit_net = (limit_up - limit_down) / total if total else 0
    break_penalty = 1.0 - min(break_rate / 100.0, 0.6) * 0.8
    lianban_signal = min(max_lianban / 10.0, 0.6) * 0.7
    yesterday_signal = max(-0.4, min(yesterday_return / 2.0, 0.5))
    risk_diff = down_over_5 / total if total else 0
    trend_strength = (new_high_20 - new_low_20) / total if total else 0
    raw = 50.0
    raw += breadth * 30.0
    raw += limit_net * 40.0
    raw += break_penalty * 10.0
    raw += lianban_signal * 10.0
    raw += yesterday_signal * 8.0
    raw += trend_strength * 10.0
    raw -= risk_diff * 35.0
    final = max(0.0, min(100.0, raw))
    return {
        "breadth": round(breadth, 4),
        "limitNet": round(limit_net, 4),
        "breakPenalty": round(break_penalty, 4),
        "lianbanSignal": round(lianban_signal, 4),
        "yesterdaySignal": round(yesterday_signal, 4),
        "riskDiff": round(risk_diff, 4),
        "trendStrength": round(trend_strength, 4),
        "raw": final,
        "final": round(final, 2),
    }


def build_sentiment_overview(today: dict[str, Any] | None, series: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    today_payload = today or {}
    series_payload = series or []
    today_score = score_breadth_components(today_payload)["final"] if today_payload else 50.0
    scores5 = [score_breadth_components(item)["final"] for item in series_payload[-5:] if item]
    scores20 = [score_breadth_components(item)["final"] for item in series_payload[-20:] if item]
    score5 = round(sum(scores5) / len(scores5), 2) if scores5 else today_score
    score20 = round(sum(scores20) / len(scores20), 2) if scores20 else today_score
    trend = "improving" if score5 > score20 + 3 else "weakening" if score5 < score20 - 3 else "sideways"
    state_label = "strong" if today_score >= 65 else "neutral" if today_score >= 45 else "weak"
    risk_diff = max(0.0, min(100.0, 50 + ((_float(today_payload.get("limitDownCount") or today_payload.get("limit_down_count") or 0) - _float(today_payload.get("limitUpCount") or today_payload.get("limit_up_count") or 0)) * 0.8))) if today_payload else 50
    return {
        "todayScore": today_score,
        "trendScore": score5,
        "riskDiffusionScore": round(risk_diff, 2),
        "state": state_label,
        "trend": trend,
        "score5": score5,
        "score20": score20,
        "limitUpCount": int(_float(today_payload.get("limitUpCount") or today_payload.get("limit_up_count") or 0)),
        "limitDownCount": int(_float(today_payload.get("limitDownCount") or today_payload.get("limit_down_count") or 0)),
        "breakRate": round(_float(today_payload.get("breakRate") or today_payload.get("break_rate") or 0), 2),
        "totalTurnover": round(_float(today_payload.get("totalTurnover") or today_payload.get("total_turnover") or 0), 2),
    }


def build_breadth_series_state(series: list[dict[str, Any]], index: int) -> dict[str, Any] | None:
    if index < 19 or index >= len(series):
        return None
    sample = series[index]
    score_today = score_breadth_components(sample)["final"]
    recent5_scores = [score_breadth_components(item)["final"] for item in series[max(0, index - 4) : index + 1]]
    recent20_scores = [score_breadth_components(item)["final"] for item in series[index - 19 : index + 1]]
    score5 = sum(recent5_scores) / len(recent5_scores) if recent5_scores else score_today
    score20 = sum(recent20_scores) / len(recent20_scores) if recent20_scores else score_today
    trend = "improving" if score5 > score20 + 3 else "weakening" if score5 < score20 - 3 else "sideways"
    state = "strong" if score_today >= 65 else "neutral" if score_today >= 45 else "weak"
    limit_up = _float(sample.get("limitUpCount") or sample.get("limit_up_count") or 0)
    limit_down = _float(sample.get("limitDownCount") or sample.get("limit_down_count") or 0)
    risk_diff = max(0.0, min(100.0, 50 + ((limit_down - limit_up) * 0.8)))
    return {
        "date": str(sample.get("date") or sample.get("timestamp") or index),
        "todayScore": round(score_today, 2),
        "score5": round(score5, 2),
        "score20": round(score20, 2),
        "trend": trend,
        "state": state,
        "riskDiffusionScore": round(risk_diff, 2),
    }


def summarize_forward_stats(samples: list[dict[str, Any]], forward_day: int) -> dict[str, Any]:
    returns = [_float(item["return"]) for item in samples if item.get("return") is not None]
    drawdowns = [_float(item["max_drawdown"]) for item in samples if item.get("max_drawdown") is not None]
    if not returns:
        return {
            "forwardDays": forward_day,
            "winRate": 0,
            "avgReturn": 0,
            "medianReturn": 0,
            "maxReturn": 0,
            "worstReturn": 0,
            "medianMaxDrawdown": 0,
            "positiveRatio": 0,
        }
    positive = [value for value in returns if value > 0]
    return {
        "forwardDays": forward_day,
        "winRate": round(len(positive) / len(returns), 4),
        "avgReturn": round(sum(returns) / len(returns), 4),
        "medianReturn": round(median(returns), 4),
        "maxReturn": round(max(returns), 4),
        "worstReturn": round(min(returns), 4),
        "medianMaxDrawdown": round(median(drawdowns), 4) if drawdowns else 0,
        "positiveRatio": round(len(positive) / len(returns), 4),
    }


def build_similar_scenario_backtest(
    benchmark_bars: list[dict[str, Any]],
    *,
    regime: str,
    window_metrics: list[dict[str, Any]],
    sentiment: dict[str, Any],
    dominant_style: str,
    index_bars_map: dict[str, list[dict[str, Any]]],
    breadth_series: list[dict[str, Any]] | None = None,
    style_mapping: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    closes = [_float(item["close"]) for item in benchmark_bars]
    if len(closes) < 130:
        return _empty_similar_scenario(regime, dominant_style, sentiment)
    breadth_payload = breadth_series or []
    ma20 = moving_average(closes, 20)
    ma60 = moving_average(closes, 60)
    wm20 = next((item for item in window_metrics if item["window"] == 20), None)
    wm60 = next((item for item in window_metrics if item["window"] == 60), None)
    current_bucket = infer_regime_bucket(
        wm20.get("rangePosition") if wm20 else None,
        wm60.get("rangePosition") if wm60 else None,
        bool(wm20.get("closeAboveMa20")) if wm20 else False,
        bool(wm20.get("closeAboveMa60")) if wm20 else False,
        wm20.get("returnN") if wm20 else None,
        wm60.get("returnN") if wm60 else None,
    )
    current = _current_scenario_vector(window_metrics, sentiment, dominant_style, current_bucket)
    candidates: list[tuple[float, int, dict[str, Any]]] = []
    upper_bound = len(benchmark_bars) - max(FORWARD_WINDOWS)
    for index in range(120, upper_bound):
        vector = _scenario_state_vector(
            benchmark_bars,
            closes,
            index,
            ma20,
            ma60,
            index_bars_map,
            breadth_payload,
            style_mapping=style_mapping,
        )
        if not vector:
            continue
        candidates.append((_scenario_distance(vector, current), index, vector))
    candidates.sort(key=lambda item: item[0])
    pool = [item for item in candidates if item[0] <= 42]
    if len(pool) < 12:
        pool = candidates[:20]
    matched = pool[:40]
    forward_stats = []
    for forward_day in FORWARD_WINDOWS:
        samples = []
        for _, index, _ in matched:
            entry = closes[index]
            exit_close = closes[index + forward_day]
            future_slice = closes[index + 1 : index + forward_day + 1]
            if not future_slice:
                continue
            samples.append({"return": exit_close / entry - 1, "max_drawdown": min(future_slice) / entry - 1})
        forward_stats.append(summarize_forward_stats(samples, forward_day))
    matched_details = [
        {
            "date": vector["date"],
            "distance": round(distance, 2),
            "regimeBucket": vector["regimeBucket"],
            "dominantStyle": vector["dominantStyle"],
            "sentimentTrend": vector["sentimentTrend"],
            "rangePos60": round(vector["shRangePos60"], 4),
            "return20": round(vector["shReturn20"], 4),
            "return60": round(vector["shReturn60"], 4),
        }
        for distance, _, vector in matched[:12]
    ]
    return {
        "lookbackWindows": WINDOWS,
        "matchedCount": len(matched),
        "matchedDates": [str(benchmark_bars[index].get("timestamp") or benchmark_bars[index].get("trade_date")) for _, index, _ in matched[:15]],
        "medianDistance": round(median([item[0] for item in matched]), 2) if matched else None,
        "matchThreshold": 42,
        "matchedDetails": matched_details,
        "forwardStats": forward_stats,
        "conclusion": {
            "regime": regime,
            "dominantStyle": dominant_style,
            "sentimentTrend": sentiment.get("trend") or "sideways",
            "matchedCount": len(matched),
        },
    }


def infer_regime_bucket(
    range_pos_20: float | None,
    range_pos_60: float | None,
    above_ma20: bool,
    above_ma60: bool,
    ret20: float | None,
    ret60: float | None,
) -> str:
    pos20 = range_pos_20 if range_pos_20 is not None else 0.5
    pos60 = range_pos_60 if range_pos_60 is not None else 0.5
    r20 = ret20 or 0
    r60 = ret60 or 0
    if above_ma20 and above_ma60 and pos60 > 0.68 and r60 > 0.04:
        return "trend_up"
    if (not above_ma20) and (not above_ma60) and pos60 < 0.32 and r60 < -0.04:
        return "trend_down"
    if pos20 > 0.78 or pos60 > 0.78:
        return "near_high"
    if pos20 < 0.22 or pos60 < 0.22:
        return "near_low"
    if abs(r20) < 0.03 and abs(r60) < 0.05:
        return "range"
    return "transition"


def _ma(cache: dict[int, list[float | None]], closes: list[float], period: int) -> list[float | None]:
    values = cache.get(period)
    if values is None:
        values = moving_average(closes, period)
        cache[period] = values
    return values


def _append_level(bucket: list[PriceLevel], latest_close: float, price: float, level_type: str, source: str, strength: int, label: str) -> None:
    if price <= 0:
        return
    bucket.append(PriceLevel(price, level_type, source, strength, label, price / latest_close - 1))


def _merge_levels(levels: list[PriceLevel], latest_close: float, level_type: str, limit: int = 8) -> list[dict[str, Any]]:
    filtered = [level for level in levels if level.price > 0]
    filtered.sort(key=lambda item: item.price)
    merged: list[PriceLevel] = []
    tolerance = max(latest_close * 0.006, 1)
    for level in filtered:
        if not merged:
            merged.append(level)
            continue
        last = merged[-1]
        if abs(level.price - last.price) <= tolerance:
            total_strength = last.strength + level.strength
            merged_price = ((last.price * last.strength) + (level.price * level.strength)) / total_strength
            merged[-1] = PriceLevel(
                merged_price,
                level_type,
                "cluster",
                min(100, total_strength),
                f"{last.label} / {level.label}",
                merged_price / latest_close - 1,
            )
        else:
            merged.append(level)
    if level_type == "support":
        ordered = [level for level in merged if level.price <= latest_close]
    else:
        ordered = [level for level in merged if level.price >= latest_close]
    ordered.sort(key=lambda item: abs(item.distance_pct))
    return [item.to_dict() for item in ordered[:limit]]


def _build_gap_levels(bars: list[dict[str, Any]], latest_close: float) -> tuple[list[PriceLevel], list[PriceLevel]]:
    supports: list[PriceLevel] = []
    resistances: list[PriceLevel] = []
    start = max(1, len(bars) - 120)
    for index in range(start, len(bars)):
        prev_high = _float(bars[index - 1]["high"])
        prev_low = _float(bars[index - 1]["low"])
        curr_high = _float(bars[index]["high"])
        curr_low = _float(bars[index]["low"])
        date_label = _format_bar_date(bars[index].get("timestamp") or bars[index].get("trade_date"))
        if curr_low > prev_high:
            gap_pct = curr_low / prev_high - 1
            if gap_pct >= 0.006:
                _append_level(supports, latest_close, prev_high, "support", "gap", min(96, 72 + int(gap_pct * 1000)), f"up gap lower {date_label}")
                _append_level(supports, latest_close, curr_low, "support", "gap", min(98, 75 + int(gap_pct * 1000)), f"up gap upper {date_label}")
        if curr_high < prev_low:
            gap_pct = prev_low / curr_high - 1
            if gap_pct >= 0.006:
                _append_level(resistances, latest_close, curr_high, "resistance", "gap", min(96, 72 + int(gap_pct * 1000)), f"down gap lower {date_label}")
                _append_level(resistances, latest_close, prev_low, "resistance", "gap", min(98, 75 + int(gap_pct * 1000)), f"down gap upper {date_label}")
    return supports, resistances


def _build_swing_cluster_levels(bars: list[dict[str, Any]], latest_close: float) -> tuple[list[PriceLevel], list[PriceLevel]]:
    supports: list[PriceLevel] = []
    resistances: list[PriceLevel] = []
    if len(bars) < 20:
        return supports, resistances
    start = max(2, len(bars) - 180)
    for index in range(start, len(bars) - 2):
        high = _float(bars[index]["high"])
        low = _float(bars[index]["low"])
        left_highs = [_float(bars[index - 2]["high"]), _float(bars[index - 1]["high"])]
        right_highs = [_float(bars[index + 1]["high"]), _float(bars[index + 2]["high"])]
        left_lows = [_float(bars[index - 2]["low"]), _float(bars[index - 1]["low"])]
        right_lows = [_float(bars[index + 1]["low"]), _float(bars[index + 2]["low"])]
        date_label = _format_bar_date(bars[index].get("timestamp") or bars[index].get("trade_date"))
        if high >= max(left_highs + right_highs):
            _append_level(resistances, latest_close, high, "resistance", "swing", 64, f"swing high {date_label}")
        if low <= min(left_lows + right_lows):
            _append_level(supports, latest_close, low, "support", "swing", 64, f"swing low {date_label}")
    return supports, resistances


def _build_volume_node_levels(bars: list[dict[str, Any]], latest_close: float) -> tuple[list[PriceLevel], list[PriceLevel]]:
    sample = bars[-120:]
    if len(sample) < 20:
        return [], []
    low_price = min(_float(item["low"]) for item in sample)
    high_price = max(_float(item["high"]) for item in sample)
    if high_price <= low_price:
        return [], []
    bin_count = 12
    step = (high_price - low_price) / bin_count
    bins = [{"weight": 0.0, "price": low_price + (index + 0.5) * step} for index in range(bin_count)]
    for item in sample:
        typical = (_float(item["high"]) + _float(item["low"]) + _float(item["close"])) / 3
        weight = _float(item.get("turnover") or item.get("amount") or item.get("volume") or 0) or 1
        index = min(bin_count - 1, max(0, int((typical - low_price) / step)))
        bins[index]["weight"] += weight
    sorted_bins = sorted(bins, key=lambda item: item["weight"], reverse=True)[:5]
    max_weight = max(item["weight"] for item in sorted_bins) if sorted_bins else 1
    supports: list[PriceLevel] = []
    resistances: list[PriceLevel] = []
    for item in sorted_bins:
        price = _float(item["price"])
        strength = 60 + int((_float(item["weight"]) / max_weight) * 30)
        if price <= latest_close:
            _append_level(supports, latest_close, price, "support", "volumeNode", strength, "volume node")
        else:
            _append_level(resistances, latest_close, price, "resistance", "volumeNode", strength, "volume node")
    return supports, resistances


def _style_relative_return(index_bars_map: dict[str, list[dict[str, Any]]], source_names: list[str], benchmark_bars: list[dict[str, Any]], window: int) -> float | None:
    valid_returns: list[float] = []
    for name in source_names:
        bars = index_bars_map.get(name)
        if not bars or len(bars) <= window or len(benchmark_bars) <= window:
            continue
        index_return = calc_return(bars, window)
        benchmark_return = calc_return(benchmark_bars, window)
        if index_return is not None and benchmark_return is not None:
            valid_returns.append(index_return - benchmark_return)
    if not valid_returns:
        return None
    return round(sum(valid_returns) / len(valid_returns), 4)


def _relative_strength_state(score: float) -> str:
    if score > 0.03:
        return "dominant"
    if score > 0.01:
        return "strong"
    if score < -0.01:
        return "weakening"
    return "neutral"


def _empty_similar_scenario(regime: str, dominant_style: str, sentiment: dict[str, Any]) -> dict[str, Any]:
    return {
        "lookbackWindows": WINDOWS,
        "matchedCount": 0,
        "matchedDates": [],
        "medianDistance": None,
        "matchThreshold": 42,
        "matchedDetails": [],
        "forwardStats": [summarize_forward_stats([], window) for window in FORWARD_WINDOWS],
        "conclusion": {
            "regime": regime,
            "dominantStyle": dominant_style,
            "sentimentTrend": sentiment.get("trend") or "sideways",
            "matchedCount": 0,
        },
    }


def _style_snapshot(
    index_bars_map: dict[str, list[dict[str, Any]]],
    benchmark_bars: list[dict[str, Any]],
    index_pos: int,
    *,
    style_mapping: list[dict[str, Any]] | None = None,
) -> dict[str, Any] | None:
    if index_pos < 60:
        return None
    short_index_map = {name: bars[: index_pos + 1] for name, bars in index_bars_map.items() if len(bars) > index_pos}
    if not short_index_map:
        return None
    rows = build_style_overview(short_index_map, benchmark_bars[: index_pos + 1], style_mapping=style_mapping)
    return {"rows": rows, "dominantStyle": dominant_style_from_rows(rows)}


def _current_scenario_vector(
    window_metrics: list[dict[str, Any]],
    sentiment: dict[str, Any],
    dominant_style: str,
    current_bucket: str,
) -> dict[str, Any]:
    wm20 = next((item for item in window_metrics if item["window"] == 20), None)
    wm60 = next((item for item in window_metrics if item["window"] == 60), None)
    return {
        "shReturn5": next((item.get("returnN") for item in window_metrics if item["window"] == 5), 0) or 0,
        "shReturn20": (wm20.get("returnN") if wm20 else 0) or 0,
        "shReturn60": (wm60.get("returnN") if wm60 else 0) or 0,
        "shReturn120": next((item.get("returnN") for item in window_metrics if item["window"] == 120), 0) or 0,
        "shRangePos5": next((item.get("rangePosition") for item in window_metrics if item["window"] == 5), 0.5) or 0.5,
        "shRangePos20": (wm20.get("rangePosition") if wm20 else 0.5) or 0.5,
        "shRangePos60": (wm60.get("rangePosition") if wm60 else 0.5) or 0.5,
        "shRangePos120": next((item.get("rangePosition") for item in window_metrics if item["window"] == 120), 0.5) or 0.5,
        "shAboveMa20": (wm20.get("closeAboveMa20") if wm20 else False),
        "shAboveMa60": (wm20.get("closeAboveMa60") if wm20 else False),
        "ma20Slope": (wm20.get("ma20Slope") if wm20 else 0) or 0,
        "ma60Slope": (wm20.get("ma60Slope") if wm20 else 0) or 0,
        "breadthTodayScore": sentiment.get("todayScore") or 50,
        "breadthTrendScore": sentiment.get("score5") or 50,
        "riskDiffusionScore": sentiment.get("riskDiffusionScore") or 50,
        "sentimentTrend": sentiment.get("trend") or "sideways",
        "dominantStyle": dominant_style,
        "regimeBucket": current_bucket,
    }


def _scenario_state_vector(
    benchmark_bars: list[dict[str, Any]],
    closes: list[float],
    index: int,
    ma20: list[float | None],
    ma60: list[float | None],
    index_bars_map: dict[str, list[dict[str, Any]]],
    breadth_series: list[dict[str, Any]],
    *,
    style_mapping: list[dict[str, Any]] | None = None,
) -> dict[str, Any] | None:
    close = closes[index]
    high5, low5 = _high_low(benchmark_bars, index, 5)
    high20, low20 = _high_low(benchmark_bars, index, 20)
    high60, low60 = _high_low(benchmark_bars, index, 60)
    high120, low120 = _high_low(benchmark_bars, index, 120)
    breadth_state = build_breadth_series_state(breadth_series, index) if len(breadth_series) > index else None
    style_state = _style_snapshot(index_bars_map, benchmark_bars, index, style_mapping=style_mapping)
    if not style_state:
        return None
    ret5 = close / closes[index - 5] - 1 if index >= 5 and closes[index - 5] else None
    ret20 = close / closes[index - 20] - 1 if closes[index - 20] else 0
    ret60 = close / closes[index - 60] - 1 if closes[index - 60] else 0
    ret120 = close / closes[index - 120] - 1 if closes[index - 120] else 0
    range_pos20 = _range_position(close, high20, low20)
    range_pos60 = _range_position(close, high60, low60)
    above_ma20 = bool(ma20[index] and close > ma20[index])
    above_ma60 = bool(ma60[index] and close > ma60[index])
    ma20_slope = (ma20[index] / ma20[index - 5] - 1) if ma20[index] and index >= 5 and ma20[index - 5] else 0
    ma60_slope = (ma60[index] / ma60[index - 20] - 1) if ma60[index] and index >= 20 and ma60[index - 20] else 0
    return {
        "date": str(benchmark_bars[index].get("timestamp") or benchmark_bars[index].get("trade_date") or index),
        "shReturn5": ret5,
        "shReturn20": ret20,
        "shReturn60": ret60,
        "shReturn120": ret120,
        "shRangePos5": _range_position(close, high5, low5),
        "shRangePos20": range_pos20,
        "shRangePos60": range_pos60,
        "shRangePos120": _range_position(close, high120, low120),
        "shAboveMa20": above_ma20,
        "shAboveMa60": above_ma60,
        "ma20Slope": ma20_slope,
        "ma60Slope": ma60_slope,
        "breadthTodayScore": breadth_state.get("todayScore") if breadth_state else 50,
        "breadthTrendScore": breadth_state.get("score5") if breadth_state else 50,
        "riskDiffusionScore": breadth_state.get("riskDiffusionScore") if breadth_state else 50,
        "sentimentTrend": breadth_state.get("trend") if breadth_state else "sideways",
        "dominantStyle": style_state["dominantStyle"],
        "regimeBucket": infer_regime_bucket(range_pos20, range_pos60, above_ma20, above_ma60, ret20, ret60),
    }


def _scenario_distance(vector: dict[str, Any], current: dict[str, Any]) -> float:
    distance = 0.0
    distance += abs((vector["shRangePos5"] or 0.5) - current["shRangePos5"]) * 6
    distance += abs((vector["shRangePos20"] or 0.5) - current["shRangePos20"]) * 10
    distance += abs((vector["shRangePos60"] or 0.5) - current["shRangePos60"]) * 16
    distance += abs((vector["shRangePos120"] or 0.5) - current["shRangePos120"]) * 8
    distance += abs((vector["shReturn5"] or 0) - current["shReturn5"]) * 120
    distance += abs((vector["shReturn20"] or 0) - current["shReturn20"]) * 80
    distance += abs((vector["shReturn60"] or 0) - current["shReturn60"]) * 55
    distance += abs((vector["shReturn120"] or 0) - current["shReturn120"]) * 35
    distance += abs(vector["ma20Slope"] - current["ma20Slope"]) * 200
    distance += abs(vector["ma60Slope"] - current["ma60Slope"]) * 150
    distance += abs((vector["breadthTodayScore"] or 50) - current["breadthTodayScore"]) * 0.22
    distance += abs((vector["breadthTrendScore"] or 50) - current["breadthTrendScore"]) * 0.15
    distance += abs((vector["riskDiffusionScore"] or 50) - current["riskDiffusionScore"]) * 0.15
    if vector["shAboveMa20"] != current["shAboveMa20"]:
        distance += 5
    if vector["shAboveMa60"] != current["shAboveMa60"]:
        distance += 7
    if vector["sentimentTrend"] != current["sentimentTrend"]:
        distance += 3.5
    if vector["regimeBucket"] != current["regimeBucket"]:
        distance += 7
    distance += style_similarity(str(vector["dominantStyle"]), str(current["dominantStyle"]))
    return distance


def _high_low(bars: list[dict[str, Any]], index: int, window: int) -> tuple[float, float]:
    sample = bars[index - window + 1 : index + 1]
    return max(_float(item["high"]) for item in sample), min(_float(item["low"]) for item in sample)


def _range_position(close: float, high: float, low: float) -> float:
    return (close - low) / (high - low) if high > low else 0.5


def _format_bar_date(value: Any) -> str:
    if value is None:
        return ""
    raw = str(value)
    try:
        number = float(raw)
        if number > 100000000000:
            number = number / 1000
        if number > 1000000000:
            return datetime.fromtimestamp(number, tz=timezone.utc).strftime("%Y-%m-%d")
    except (TypeError, ValueError):
        pass
    if len(raw) >= 10 and raw[:4].isdigit():
        return raw[:10]
    return raw


def _float(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0
