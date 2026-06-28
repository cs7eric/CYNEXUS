from __future__ import annotations

from datetime import date, timedelta

from cynexus_data.domain.market.market_technical import (
    build_industry_strength_overview,
    build_sentiment_overview,
    build_similar_scenario_backtest,
    build_style_overview,
    build_support_resistance,
    dominant_style_from_rows,
    infer_regime_bucket,
    latest_window_metrics,
    moving_average,
    score_breadth_components,
    summarize_forward_stats,
)


def _bars(count: int = 80) -> list[dict]:
    start = date(2026, 1, 1)
    rows = []
    price = 100.0
    for index in range(count):
        price += 0.4
        rows.append(
            {
                "trade_date": (start + timedelta(days=index)).isoformat(),
                "open": price - 0.3,
                "high": price + 1.0,
                "low": price - 1.0,
                "close": price,
                "volume": 1000 + index,
                "amount": (1000 + index) * price,
            }
        )
    rows[30]["low"] = rows[29]["high"] + 1.0
    rows[30]["high"] = rows[30]["low"] + 2.0
    rows[30]["close"] = rows[30]["low"] + 1.0
    return rows


def _trend_bars(count: int, step: float = 0.4, start_price: float = 100.0) -> list[dict]:
    start = date(2025, 1, 1)
    rows = []
    price = start_price
    for index in range(count):
        price += step + ((index % 7) - 3) * 0.05
        rows.append(
            {
                "trade_date": (start + timedelta(days=index)).isoformat(),
                "open": price - 0.2,
                "high": price + 0.8,
                "low": price - 0.8,
                "close": price,
                "volume": 1000 + index * 3,
                "amount": (1000 + index * 3) * price,
            }
        )
    return rows


def test_moving_average_matches_window() -> None:
    assert moving_average([1, 2, 3, 4], 3) == [None, None, 2.0, 3.0]


def test_latest_window_metrics_computes_range_state() -> None:
    metrics = latest_window_metrics(_bars())
    by_window = {item["window"]: item for item in metrics}

    assert 20 in by_window
    assert by_window[20]["returnN"] is not None
    assert by_window[20]["closeAboveMa20"] is True


def test_build_support_resistance_returns_nearest_levels() -> None:
    bars = _bars()
    metrics = latest_window_metrics(bars)
    supports, resistances = build_support_resistance(bars, metrics, ma20=125.0, ma60=112.0)

    assert supports
    assert supports[0]["type"] == "support"
    assert any(item["source"] in {"ma", "cluster", "rangeHighLow", "gap", "swing", "volumeNode"} for item in supports)
    assert all(item["price"] <= bars[-1]["close"] for item in supports)
    assert isinstance(resistances, list)


def test_infer_regime_bucket() -> None:
    assert infer_regime_bucket(0.8, 0.75, True, True, 0.05, 0.08) == "trend_up"
    assert infer_regime_bucket(0.1, 0.2, False, False, -0.05, -0.08) == "trend_down"


def test_style_overview_and_dominant_style() -> None:
    benchmark = _trend_bars(90, step=0.2)
    index_map = {
        "fast": _trend_bars(90, step=0.5),
        "slow": _trend_bars(90, step=0.05),
    }

    rows = build_style_overview(index_map, benchmark, style_mapping=[{"style": "fast_style", "sources": ["fast"]}, {"style": "slow_style", "sources": ["slow"]}])

    assert rows[0]["relativeReturn20"] is not None
    assert rows[0]["state"] in {"dominant", "strong"}
    assert dominant_style_from_rows(rows) == "fast_style"


def test_industry_strength_overview_sorts_by_score() -> None:
    benchmark = _trend_bars(90, step=0.2)
    industries = [{"name": "Fast", "symbol": "fast_idx"}, {"name": "Slow", "symbol": "slow_idx"}]
    index_map = {"fast_idx": _trend_bars(90, step=0.5), "slow_idx": _trend_bars(90, step=0.05)}

    rows = build_industry_strength_overview(industries, benchmark, index_map)

    assert [row["name"] for row in rows] == ["Fast", "Slow"]
    assert rows[0]["score"] > rows[1]["score"]


def test_sentiment_scoring_and_overview() -> None:
    today = {
        "upCount": 3200,
        "downCount": 1500,
        "limitUpCount": 80,
        "limitDownCount": 5,
        "totalCount": 5000,
        "breakRate": 12,
        "maxLianBan": 6,
        "new20HighCount": 400,
        "new20LowCount": 50,
    }
    series = [{**today, "upCount": 2500 + index * 10, "downCount": 2000 - index * 5} for index in range(20)]

    components = score_breadth_components(today)
    overview = build_sentiment_overview(today, series)

    assert components["final"] > 50
    assert overview["todayScore"] == components["final"]
    assert overview["state"] in {"strong", "neutral", "weak"}


def test_forward_stats_and_similar_scenarios() -> None:
    benchmark = _trend_bars(220, step=0.18)
    metrics = latest_window_metrics(benchmark)
    index_map = {"fast": _trend_bars(220, step=0.2)}
    style_mapping = [{"style": "fast_style", "sources": ["fast"]}]
    breadth_series = [{"upCount": 2500 + (index % 20), "downCount": 2000, "limitUpCount": 30, "limitDownCount": 5, "totalCount": 5000} for index in range(220)]
    sentiment = build_sentiment_overview(breadth_series[-1], breadth_series)
    dominant_style = dominant_style_from_rows(build_style_overview(index_map, benchmark, style_mapping=style_mapping))

    stats = summarize_forward_stats([{"return": 0.05, "max_drawdown": -0.02}, {"return": -0.01, "max_drawdown": -0.03}], 5)
    backtest = build_similar_scenario_backtest(
        benchmark,
        regime="trend",
        window_metrics=metrics,
        sentiment=sentiment,
        dominant_style=dominant_style,
        index_bars_map=index_map,
        breadth_series=breadth_series,
        style_mapping=style_mapping,
    )

    assert stats["winRate"] == 0.5
    assert backtest["matchedCount"] >= 0
    assert len(backtest["forwardStats"]) == 9
