from __future__ import annotations

from cynexus_data.domain.stock.turnover import build_turnover_series, estimate_turnover_rate, latest_turnover_entry


def test_estimate_turnover_rate_matches_legacy_formula() -> None:
    assert estimate_turnover_rate(12000, 1_000_000_000) == 0.12
    assert estimate_turnover_rate(0, 1_000_000_000) == 0.0
    assert estimate_turnover_rate(12000, None) == 0.0


def test_build_turnover_series_from_daily_bars() -> None:
    series = build_turnover_series(
        symbol="000001",
        bars=[
            {"trade_date": "2026-06-25", "volume": 10000, "amount": 100000000},
            {"trade_date": "2026-06-26", "volume": 12000, "amount": 120000000},
        ],
        circulating_shares=1_000_000_000,
        total_shares=1_200_000_000,
    )

    assert series["entries"][1]["turnover_rate"] == 0.12
    assert latest_turnover_entry(series)["trade_date"] == "2026-06-26"
