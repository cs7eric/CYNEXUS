from __future__ import annotations

import pandas as pd

from cynexus_data.sources.akshare.client import AkshareClient


class _FakeAkshare:
    def stock_zh_a_spot_em(self):
        return pd.DataFrame(
            [
                {"代码": "600000", "涨跌幅": 1.2, "成交额": 100_000_000, "成交量": 20_000},
                {"代码": "000001", "涨跌幅": -0.8, "成交额": 200_000_000, "成交量": 30_000},
                {"代码": "300001", "涨跌幅": 9.8, "成交额": 300_000_000, "成交量": 40_000},
            ]
        )

    def stock_market_fund_flow(self):
        return pd.DataFrame(
            [
                {
                    "主力净流入-净占比": 1.1,
                    "超大单净流入-净额": 100_000_000,
                    "超大单净流入-净占比": 0.6,
                    "大单净流入-净额": 200_000_000,
                    "大单净流入-净占比": 0.5,
                    "中单净流入-净额": -50_000_000,
                    "中单净流入-净占比": -0.2,
                    "小单净流入-净额": -250_000_000,
                    "小单净流入-净占比": -0.9,
                }
            ]
        )


def test_akshare_market_overview_summarizes_spot_and_flow(monkeypatch) -> None:
    monkeypatch.setattr(AkshareClient, "require_module", lambda self: _FakeAkshare())

    payload = AkshareClient().fetch_market_overview()

    assert payload["totalAmount"] == 6.0
    assert payload["totalVolume"] == 9.0
    assert payload["risingCount"] == 2
    assert payload["fallingCount"] == 1
    assert payload["limitUpCount"] == 1
    assert payload["stockCount"] == 3
    assert payload["mainNetInflow"] == 3.0
    assert payload["superLargeNetInflow"] == 1.0
    assert payload["largeNetInflowRatio"] == 0.5
