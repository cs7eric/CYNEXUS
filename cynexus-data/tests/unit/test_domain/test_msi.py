from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.msi import calculate_market_sentiment_index, msi_level, percentile_score, sector_breadth_score


def test_calculate_market_sentiment_index_uses_neutral_for_missing_components() -> None:
    payload = calculate_market_sentiment_index(trade_date=date(2026, 6, 26), components={"vol": 80, "turnover": 60})

    assert payload is not None
    assert payload["componentCount"] == 2
    assert payload["components"]["breadth"] is None
    assert payload["compositeScore"] == 56.0
    assert payload["level"] == "active"


def test_calculate_market_sentiment_index_require_full_returns_none() -> None:
    payload = calculate_market_sentiment_index(trade_date=date(2026, 6, 26), components={"vol": 80}, require_full=True)

    assert payload is None


def test_msi_helpers() -> None:
    assert percentile_score([1, 2, 3, 4], 3) == 50.0
    assert sector_breadth_score(advancing=72, total=90) == 80.0
    assert msi_level(29.9) == "ice"
    assert msi_level(70) == "hot"
