from __future__ import annotations

from cynexus_data.quality.checks import summarize_ohlcv_issues, validate_ohlcv_record


def test_validate_ohlcv_record_accepts_valid_bar() -> None:
    issues = validate_ohlcv_record({"open": 10, "high": 12, "low": 9, "close": 11, "volume": 100, "amount": 1000})

    assert issues == []


def test_summarize_ohlcv_issues_counts_invalid_prices() -> None:
    summary = summarize_ohlcv_issues(
        [
            {"open": 10, "high": 9, "low": 8, "close": 8.5, "volume": 1, "amount": 1},
            {"open": 10, "high": 12, "low": 11, "close": 10.5, "volume": -1, "amount": -1},
        ]
    )

    assert summary == {"high_below_price": 1, "low_above_price": 1, "negative_volume": 1, "negative_amount": 1}
