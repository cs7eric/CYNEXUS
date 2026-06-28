from __future__ import annotations

from cynexus_data.sources.tdx.sector_quote import fetch_sector_quote_detail, parse_theme_market_table


class Table:
    def __init__(self, rows):
        self.rows = rows


class Response:
    ok = True

    def __init__(self, rows):
        self.tables = [Table([]), Table(rows)]


class FakeF10:
    def theme_market(self, code, req_id, page=-1, page_size=-1, extra=None):
        if req_id == "200741":
            return Response([{"N001": "0", "N002": "1.2", "N003": "0.8", "N004": "0"}])
        if req_id == "200744":
            return Response([{"N001": "SH", "N002": "600000", "N003": "浦发银行", "N004": "2.5", "N005": "10.1"}])
        return Response([])


class FakeClient:
    f10 = FakeF10()


def test_parse_theme_market_period_stats() -> None:
    rows = parse_theme_market_table(Response([{"N001": "3", "N002": "2", "N003": "1", "N004": "0"}]), "period_stats")

    assert rows[0]["range_label"] == "近20日"
    assert rows[0]["value1"] == 2.0


def test_parse_theme_market_constituents_sorts_by_change_pct() -> None:
    rows = parse_theme_market_table(
        Response([
            {"N001": "SH", "N002": "600001", "N003": "A", "N004": "-1", "N005": "9"},
            {"N001": "SH", "N002": "600000", "N003": "B", "N004": "2", "N005": "10"},
        ]),
        "constituents",
    )

    assert rows[0]["code"] == "600000"


def test_fetch_sector_quote_detail_calls_requested_datasets() -> None:
    detail = fetch_sector_quote_detail(FakeClient(), code6="880471", name="银行", seed_code="sh600000", datasets=["period_stats", "constituents"])

    assert detail["ok"] is True
    assert detail["sections"]["period_stats"][0]["range_code"] == "0"
    assert detail["sections"]["constituents"][0]["name"] == "浦发银行"
