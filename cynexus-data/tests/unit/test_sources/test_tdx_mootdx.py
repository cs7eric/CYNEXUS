from __future__ import annotations

import pandas as pd

from cynexus_data.sources.provider_fallback import fetch_daily_kline_with_fallback, fetch_minute_kline_with_fallback
from cynexus_data.sources.tdx import mootdx


def test_normalize_mootdx_frame() -> None:
    frame = pd.DataFrame(
        [
            {"datetime": "2026-06-26 09:31:00", "open": 10, "close": 10.5, "high": 11, "low": 9, "volume": 20, "amount": 200},
            {"datetime": "2026-06-26 09:32:00", "open": 10.5, "close": 11, "high": 12, "low": 10, "volume": 40, "amount": 500},
        ]
    )

    rows = mootdx.normalize_mootdx_frame(frame)

    assert rows[0]["trade_time"].hour == 9
    assert rows[0]["source"] == "mootdx"
    assert rows[0]["volume_ratio"] is None
    assert rows[1]["volume_ratio"] == 2.0


def test_fetch_mootdx_kline_rows_rejects_adjusted_minute() -> None:
    class Client:
        pass

    try:
        mootdx.fetch_mootdx_kline_rows(Client(), symbol="600000", period="1m", adjust="qfq")
    except ValueError as exc:
        assert "adjust=none" in str(exc)
    else:  # pragma: no cover - explicit guard
        raise AssertionError("expected ValueError")


def test_provider_fallback_can_use_mootdx_daily(monkeypatch) -> None:
    class Hooks:
        pass

    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.create_mootdx_client",
        lambda: object(),
    )
    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.fetch_mootdx_kline_rows",
        lambda client, **kwargs: [{"close": 1, "source": "mootdx", "kwargs": kwargs}],
    )

    result = fetch_daily_kline_with_fallback(Hooks(), symbol="600000", providers=["mootdx"], adjust="qfq", limit=10)

    assert result.source == "mootdx"
    assert result.items[0]["kwargs"]["period"] == "day"
    assert result.items[0]["kwargs"]["adjust"] == "qfq"


def test_provider_fallback_can_use_mootdx_minute(monkeypatch) -> None:
    class Hooks:
        pass

    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.create_mootdx_client",
        lambda: object(),
    )
    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.fetch_mootdx_kline_rows",
        lambda client, **kwargs: [{"close": 1, "source": "mootdx", "kwargs": kwargs}],
    )

    result = fetch_minute_kline_with_fallback(Hooks(), symbol="600000", providers=["mootdx"], period=5, limit=10)

    assert result.source == "mootdx"
    assert result.items[0]["kwargs"]["period"] == "5m"
    assert result.items[0]["kwargs"]["adjust"] == "none"
