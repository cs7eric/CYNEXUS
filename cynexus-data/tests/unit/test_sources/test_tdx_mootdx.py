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


def test_mootdx_server_candidates_use_env_first(monkeypatch) -> None:
    monkeypatch.setenv("CYNEXUS_MOOTDX_SERVER", "1.2.3.4:7711")
    monkeypatch.setattr(mootdx, "_mootdx_config_servers", lambda: [("5.6.7.8", 7709)])

    candidates = mootdx.mootdx_server_candidates()

    assert candidates[0] == ("1.2.3.4", 7711)
    assert ("5.6.7.8", 7709) in candidates


def test_mootdx_server_candidates_dedupe_explicit_server(monkeypatch) -> None:
    monkeypatch.setenv("CYNEXUS_MOOTDX_SERVER", "1.2.3.4:7711")

    candidates = mootdx.mootdx_server_candidates(("9.9.9.9", 7709))

    assert candidates == [("9.9.9.9", 7709)]


def test_provider_fallback_can_use_mootdx_daily(monkeypatch) -> None:
    class Hooks:
        pass

    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.create_mootdx_client",
        lambda **kwargs: object(),
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
        lambda **kwargs: object(),
    )
    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.fetch_mootdx_kline_rows",
        lambda client, **kwargs: [{"close": 1, "source": "mootdx", "kwargs": kwargs}],
    )

    result = fetch_minute_kline_with_fallback(Hooks(), symbol="600000", providers=["mootdx"], period=5, limit=10)

    assert result.source == "mootdx"
    assert result.items[0]["kwargs"]["period"] == "5m"
    assert result.items[0]["kwargs"]["adjust"] == "none"


def test_provider_fallback_passes_index_target_type_to_mootdx_minute(monkeypatch) -> None:
    class Hooks:
        pass

    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.create_mootdx_client",
        lambda **kwargs: object(),
    )
    monkeypatch.setattr(
        "cynexus_data.sources.provider_fallback.fetch_mootdx_kline_rows",
        lambda client, **kwargs: [{"close": 1, "source": "mootdx", "kwargs": kwargs}],
    )

    result = fetch_minute_kline_with_fallback(
        Hooks(),
        symbol="880301",
        providers=["mootdx"],
        period=1,
        limit=10,
        target_type="sector",
    )

    assert result.source == "mootdx"
    assert result.items[0]["kwargs"]["target_type"] == "index"
