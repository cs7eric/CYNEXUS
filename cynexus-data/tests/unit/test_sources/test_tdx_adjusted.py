from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from cynexus_data.sources.tdx.adjusted import fetch_adjusted_daily_frame, stock_symbol_to_tdx_code


@dataclass
class FakeBar:
    time: datetime
    open: float
    high: float
    low: float
    close: float
    volume_lots: int
    amount: float


class FakeSeries:
    def __init__(self, bars):
        self.bars = bars


class FakeClient:
    def get_adjusted_kline_all(self, period, code, adjust, page_size=800, max_pages=200):
        assert period == "day"
        assert code == "sz000001"
        assert adjust == "qfq"
        return FakeSeries([FakeBar(datetime(2026, 6, 26), 10, 11, 9, 10.5, 100, 1000.0)])


def test_stock_symbol_to_tdx_code() -> None:
    assert stock_symbol_to_tdx_code("600000") == "sh600000"
    assert stock_symbol_to_tdx_code("000001") == "sz000001"
    assert stock_symbol_to_tdx_code("399001", target_type="index") == "sz399001"


def test_fetch_adjusted_daily_frame_from_client() -> None:
    frame = fetch_adjusted_daily_frame(FakeClient(), "000001", "qfq")

    assert frame["market"].tolist() == ["SZ"]
    assert frame["symbol"].tolist() == ["000001"]
    assert frame["close"].tolist() == [10.5]
    assert frame["adjust"].tolist() == ["qfq"]
