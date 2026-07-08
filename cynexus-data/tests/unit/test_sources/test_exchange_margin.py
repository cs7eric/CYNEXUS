from __future__ import annotations

from datetime import date
from io import BytesIO

import pandas as pd

from cynexus_data.sources.exchange_margin.client import ExchangeMarginClient


class FakeResponse:
    def __init__(self, payload=None, content: bytes = b""):
        self.payload = payload
        self.content = content

    def raise_for_status(self):
        return None

    def json(self):
        return self.payload


class FakeSession:
    def get(self, url, params=None, headers=None, timeout=None):
        if "queryMargin.do" in url and params.get("tabType") == "":
            return FakeResponse(
                {
                    "result": [
                        [
                            "",
                            "20260703",
                            "",
                            "3",
                            "2",
                            "20",
                            "",
                            "",
                            "10",
                            "120",
                            "100",
                            "",
                            "",
                        ]
                    ]
                }
            )
        if "queryMargin.do" in url and params.get("tabType") == "mxtype":
            return FakeResponse(
                {
                    "result": [
                        [
                            "",
                            "20260703",
                            "1",
                            "3",
                            "2",
                            "",
                            "",
                            "8",
                            "10",
                            "",
                            "100",
                            "浦发银行",
                            "600000",
                        ]
                    ]
                }
            )
        if "ShowReport/data" in url:
            return FakeResponse(
                [
                    {
                        "data": [
                            {
                                "a": "11",
                                "b": "101",
                                "c": "4",
                                "d": "5",
                                "e": "21",
                                "f": "122",
                            }
                        ]
                    }
                ]
            )
        frame = pd.DataFrame(
            [
                {
                    "证券代码": "000001",
                    "证券简称": "平安银行",
                    "融资买入额": "11",
                    "融资余额": "101",
                    "融券卖出量": "4",
                    "融券余量": "5",
                    "融券余额": "21",
                    "融资融券余额": "122",
                }
            ]
        )
        out = BytesIO()
        frame.to_excel(out, index=False)
        return FakeResponse(content=out.getvalue())


def test_exchange_margin_client_normalizes_summary_and_detail() -> None:
    client = ExchangeMarginClient(FakeSession())

    summary = client.fetch_margin_market_summary(date(2026, 7, 3))
    detail = client.fetch_margin_stock_detail(date(2026, 7, 3))

    assert [row["source"] for row in summary] == ["exchange.sse.margin_summary", "exchange.szse.margin_summary"]
    assert summary[0]["margin_balance"] == 0.000001
    assert detail[0]["stock_code"] == "600000"
    assert detail[1]["stock_code"] == "000001"
    assert detail[0]["extra"]["provider"] == "exchange"
