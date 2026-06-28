from __future__ import annotations

from datetime import datetime

from cynexus_data.domain.stock.aggregate_kline import aggregate_ohlcv


def test_aggregate_ohlcv_to_5m() -> None:
    rows = [
        {"trade_time": datetime(2026, 6, 26, 9, 31), "open": 10, "high": 11, "low": 9, "close": 10.5, "volume": 1, "amount": 10},
        {"trade_time": datetime(2026, 6, 26, 9, 34), "open": 10.5, "high": 12, "low": 10, "close": 11, "volume": 2, "amount": 20},
    ]
    [bar] = aggregate_ohlcv(rows, 5)
    assert bar["open"] == 10
    assert bar["high"] == 12
    assert bar["low"] == 9
    assert bar["close"] == 11
    assert bar["volume"] == 3


def test_aggregate_ohlcv_groups_by_symbol_and_source() -> None:
    rows = [
        {"market": "SZ", "symbol": "000001", "source": "tdx", "trade_time": datetime(2026, 6, 26, 9, 31), "open": 10, "high": 11, "low": 9, "close": 10.5, "volume": 1, "amount": 10},
        {"market": "SZ", "symbol": "000001", "source": "tdx", "trade_time": datetime(2026, 6, 26, 9, 32), "open": 10.5, "high": 12, "low": 10, "close": 11, "volume": 2, "amount": 20},
        {"market": "SZ", "symbol": "000002", "source": "tdx", "trade_time": datetime(2026, 6, 26, 9, 32), "open": 20, "high": 21, "low": 19, "close": 20.5, "volume": 3, "amount": 30},
        {"market": "SZ", "symbol": "000001", "source": "sina", "trade_time": datetime(2026, 6, 26, 9, 32), "open": 30, "high": 31, "low": 29, "close": 30.5, "volume": 4, "amount": 40},
    ]

    bars = aggregate_ohlcv(rows, 5)

    assert len(bars) == 3
    first = next(bar for bar in bars if bar["symbol"] == "000001" and bar["source"] == "tdx")
    assert first["open"] == 10
    assert first["close"] == 11
    assert first["volume"] == 3
