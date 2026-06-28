from __future__ import annotations

from cynexus_data.sources.eastmoney.client import EastMoneyClient, eastmoney_secid_candidates, parse_stock_meta, summarize_market_breadth


def test_summarize_eastmoney_market_breadth() -> None:
    payload = summarize_market_breadth(
        [
            {"f3": 1.2, "f5": 10_000, "f6": 100_000_000, "f12": "600000"},
            {"f3": -10.1, "f5": 20_000, "f6": 200_000_000, "f12": "000001"},
            {"f3": 9.8, "f5": 30_000, "f6": 300_000_000, "f12": "300001"},
            {"f3": 0, "f5": 40_000, "f6": 400_000_000, "f12": "688001"},
        ],
        pages=1,
    )

    assert payload["totalAmount"] == 10.0
    assert payload["totalVolume"] == 10.0
    assert payload["risingCount"] == 2
    assert payload["fallingCount"] == 1
    assert payload["flatCount"] == 1
    assert payload["limitUpCount"] == 1
    assert payload["limitDownCount"] == 1
    assert payload["stockCount"] == 4
    assert payload["pages"] == 1


def test_eastmoney_client_fetch_market_breadth_pages_until_short_page() -> None:
    class Response:
        def __init__(self, payload):
            self.payload = payload

        def raise_for_status(self):
            return None

        def json(self):
            return self.payload

    class Session:
        calls = []

        def get(self, url, params=None, timeout=None):
            self.calls.append(params)
            if params["pn"] == "1":
                return Response({"data": {"diff": [{"f3": 1, "f5": 10_000, "f6": 100_000_000}]}})
            return Response({"data": {"diff": []}})

    session = Session()
    client = EastMoneyClient(session=session, base_url="https://push2.eastmoney.com")

    payload = client.fetch_market_breadth(page_size=1, max_pages=3)

    assert len(session.calls) == 2
    assert session.calls[0]["fields"] == "f2,f3,f5,f6,f12"
    assert payload["risingCount"] == 1
    assert payload["stockCount"] == 1


def test_parse_eastmoney_stock_meta_and_secid_candidates() -> None:
    meta = parse_stock_meta(
        {"f57": "600000", "f58": "浦发银行", "f20": 400_000_000_000, "f21": 300_000_000_000, "f100": "银行"},
        fallback_code="600000",
    )

    assert eastmoney_secid_candidates("600000") == ["1.600000", "0.600000"]
    assert eastmoney_secid_candidates("000001") == ["0.000001", "1.000001"]
    assert meta["code"] == "600000"
    assert meta["name"] == "浦发银行"
    assert meta["industry"] == "银行"
    assert meta["total_market_cap"] == 400_000_000_000
    assert meta["float_market_cap"] == 300_000_000_000
