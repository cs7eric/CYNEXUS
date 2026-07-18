from __future__ import annotations

from cynexus_data.sources.eastmoney.client import (
    EastMoneyClient,
    eastmoney_secid_candidates,
    parse_market_fund_flow_rows,
    parse_stock_meta,
    summarize_market_breadth,
)


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


def test_parse_market_fund_flow_rows() -> None:
    rows = parse_market_fund_flow_rows(
        [
            "2026-07-16,-83885834240,67088457728,16797376512,-40939352064,-42946482176,-3.49,2.79,0.70,-1.70,-1.79,3882.41,-1.85,14488.65,-1.97"
        ]
    )

    assert rows == [
        {
            "trade_date": "2026-07-16",
            "main_net_inflow": -83885834240.0,
            "small_net_inflow": 67088457728.0,
            "medium_net_inflow": 16797376512.0,
            "large_net_inflow": -40939352064.0,
            "super_large_net_inflow": -42946482176.0,
            "main_net_inflow_ratio": -3.49,
            "small_net_inflow_ratio": 2.79,
            "medium_net_inflow_ratio": 0.7,
            "large_net_inflow_ratio": -1.7,
            "super_large_net_inflow_ratio": -1.79,
            "sh_close": 3882.41,
            "sh_change_pct": -1.85,
            "sz_close": 14488.65,
            "sz_change_pct": -1.97,
        }
    ]


def test_eastmoney_client_fetch_market_fund_flow() -> None:
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"data": {"klines": ["2026-07-16,-1,2,3,-4,-5,-0.1,0.2,0.3,-0.4,-0.5,1,2,3,4"]}}

    class Session:
        def get(self, url, params=None, headers=None, timeout=None):
            assert url.endswith("/api/qt/stock/fflow/daykline/get")
            assert params["secid"] == "1.000001"
            assert params["secid2"] == "0.399001"
            assert headers["Referer"].endswith("dpzjlx.html")
            return Response()

    rows = EastMoneyClient(session=Session(), base_url="https://push2.eastmoney.com").fetch_market_fund_flow(limit=5)

    assert len(rows) == 1
    assert rows[0]["main_net_inflow"] == -1.0


def test_eastmoney_client_uses_browser_context_after_http_failure(monkeypatch) -> None:
    class Session:
        def get(self, url, params=None, headers=None, timeout=None):
            raise RuntimeError("remote disconnected")

    class Browser:
        def request_json(self, url, *, params, headers):
            assert url.startswith("https://push2his.eastmoney.com/")
            assert params["secid"] == "1.000001"
            return {"data": {"klines": ["2026-07-16,-1,2,3,-4,-5,-0.1,0.2,0.3,-0.4,-0.5,1,2,3,4"]}}

    from cynexus_data.sources.eastmoney import client as client_module

    monkeypatch.setattr(client_module, "browser_session", Browser())
    client = EastMoneyClient(session=Session(), base_url="https://push2.eastmoney.com")

    rows = client.fetch_market_fund_flow(limit=5)

    assert len(rows) == 1
    assert client.last_market_fund_flow_metadata == {
        "fallback_used": True,
        "source_mode": "browser_context",
        "stale": False,
        "error": None,
        "http_error": "remote disconnected",
    }


def test_eastmoney_client_reports_both_source_failures(monkeypatch) -> None:
    class Session:
        def get(self, url, params=None, headers=None, timeout=None):
            raise RuntimeError("remote disconnected")

    class Browser:
        def request_json(self, url, *, params, headers):
            raise RuntimeError("playwright unavailable")

    from cynexus_data.sources.eastmoney import client as client_module

    monkeypatch.setattr(client_module, "browser_session", Browser())
    client = EastMoneyClient(session=Session(), base_url="https://push2.eastmoney.com")

    try:
        client.fetch_market_fund_flow(limit=5)
    except RuntimeError as exc:
        assert "remote disconnected" in str(exc)
        assert "playwright unavailable" in str(exc)
    else:
        raise AssertionError("expected market fund-flow fetch to fail")

    assert client.last_market_fund_flow_metadata["source_mode"] == "failed"
    assert client.last_market_fund_flow_metadata["stale"] is True
