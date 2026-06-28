from __future__ import annotations

from datetime import datetime

from cynexus_data.sources.tdx.market_overview import fetch_eltdx_market_overview


class FakeClient:
    def get_a_share_codes_all(self):
        return ["sh600000", "sz000001", "sz300001"]

    def get_quote(self, codes):
        return [
            {"amount": 100_000_000, "last_price": 11.0, "pre_close_price": 10.0},
            {"amount": 200_000_000, "last_price": 9.0, "pre_close_price": 10.0},
            {"amount": 300_000_000, "last_price": 10.0, "pre_close_price": 10.0},
        ]


def test_fetch_eltdx_market_overview_summarizes_quotes() -> None:
    payload = fetch_eltdx_market_overview(FakeClient(), captured_at=datetime(2026, 6, 26, 15, 0, 0))

    assert payload["totalAmount"] == 6.0
    assert payload["risingCount"] == 1
    assert payload["fallingCount"] == 1
    assert payload["flatCount"] == 1
    assert payload["limitUpCount"] == 1
    assert payload["limitDownCount"] == 1
    assert payload["stockCount"] == 3
    assert payload["tradingDate"] == "2026-06-26"
