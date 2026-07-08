from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from cynexus_data.sources.eastmoney import a_stock as em
from cynexus_data.sources.eastmoney.http import diff_rows, parse_jsonp
from cynexus_data.sources.iwencai.client import dedup_articles
from cynexus_data.sources.sina.a_stock import SinaAStockClient


@dataclass
class FakeResponse:
    payload: dict[str, Any] | None = None
    text: str = ""

    def json(self) -> dict[str, Any]:
        return self.payload or {}

    def raise_for_status(self) -> None:
        return None


class FakeEastMoneyClient:
    def __init__(self, payloads: list[dict[str, Any]]):
        self.payloads = list(payloads)
        self.calls: list[dict[str, Any]] = []

    def get(self, url: str, **kwargs):
        self.calls.append({"url": url, **kwargs})
        return FakeResponse(payload=self.payloads.pop(0))

    def post(self, url: str, **kwargs):
        self.calls.append({"url": url, **kwargs})
        return FakeResponse(payload=self.payloads.pop(0))


class FakeSinaSession:
    def get(self, url: str, **kwargs):
        return FakeResponse(text='var hq_str_CON_SO_10000000="购1000,,, ,12,0.5,0.1,-0.02,0.03,0.1735,1.2,0.8,10000000,2.5,0.10,0.11";')


def test_parse_jsonp_and_diff_rows_handles_provider_shapes():
    assert parse_jsonp('callback({"ok":true});') == {"ok": True}
    assert diff_rows({"data": {"diff": {"0": {"f12": "600519"}, "1": {"f12": "000001"}}}}) == [{"f12": "600519"}, {"f12": "000001"}]


def test_eastmoney_concept_blocks_normalizes_board_list():
    client = FakeEastMoneyClient([{"data": {"diff": [{"f14": "白酒", "f12": "BK0896", "f3": 1.2, "f128": "贵州茅台"}]}}])
    payload = em.concept_blocks(client, "600519")
    assert payload["total"] == 1
    assert payload["concept_tags"] == ["白酒"]
    assert client.calls[0]["params"]["spt"] == "3"


def test_eastmoney_limit_sentiment_uses_four_pool_contract_shape():
    client = FakeEastMoneyClient(
        [
            {"data": {"pool": [{"c": "000001", "n": "平安银行", "p": 10000, "zdp": 10, "lbc": 2, "fbt": 92500, "lbt": 93000, "fund": 1, "zbc": 0}]}},
            {"data": {"pool": [{"c": "000002", "n": "万科A", "p": 9000, "zdp": 7, "fbt": 93000, "zbc": 1}]}},
            {"data": {"pool": []}},
        ]
    )
    payload = em.limit_sentiment(client, "20260706")
    assert payload["zt_count"] == 1
    assert payload["zb_count"] == 1
    assert payload["break_rate"] == 50.0
    assert payload["ladder"] == {2: 1}


def test_iwencai_dedup_keeps_highest_score_per_uid():
    rows = dedup_articles(
        [
            {"uid": "a", "score": 1, "publish_date": "2026-01-01"},
            {"uid": "a", "score": 3, "publish_date": "2026-01-02"},
            {"uid": "b", "score": 2, "publish_date": "2025-01-01"},
        ]
    )
    assert [row["uid"] for row in rows] == ["a", "b"]
    assert rows[0]["score"] == 3


def test_sina_option_greeks_skips_empty_fields():
    client = SinaAStockClient(session=FakeSinaSession())
    payload = client.option_greeks("10000000")
    assert payload["delta"] == 0.5
    assert payload["iv"] == 0.1735
    assert payload["strike"] == 2.5
