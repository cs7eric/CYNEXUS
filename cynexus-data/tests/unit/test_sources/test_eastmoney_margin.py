from __future__ import annotations

from datetime import date

from cynexus_data.sources.eastmoney import a_stock


def test_normalize_margin_row_preserves_all_margin_fields() -> None:
    row = a_stock._normalize_margin_row({
        "DATE": "2026-07-27T00:00:00",
        "SCODE": "600000",
        "SECNAME": "浦发银行",
        "RZYE": 101,
        "RZMRE": 12,
        "RZCHE": 3,
        "RQYL": 4,
        "RQYE": 5,
        "RQMCL": 6,
        "RQCHL": 7,
        "RZRQYE": 106,
    })

    assert row == {
        "date": "2026-07-27",
        "stock_code": "600000",
        "stock_name": "浦发银行",
        "rzye": 101,
        "rzmre": 12,
        "rzche": 3,
        "rqyl": 4,
        "rqye": 5,
        "rqmcl": 6,
        "rqchl": 7,
        "rzrqye": 106,
    }


def test_margin_trading_latest_reads_multiple_pages_for_target_day(monkeypatch) -> None:
    pages = {
        1: [{"DATE": "2026-07-28", "SCODE": "600000"}],
        2: [{"DATE": "2026-07-28", "SCODE": "000001"}],
        3: [],
    }
    calls = []

    def fake_datacenter(client, report_name, **kwargs):
        calls.append(kwargs["page_number"])
        return pages[kwargs["page_number"]]

    monkeypatch.setattr(a_stock, "datacenter", fake_datacenter)

    rows = a_stock.margin_trading_latest(object(), date(2026, 7, 28), page_size=1, max_pages=5)

    assert [row["stock_code"] for row in rows] == ["600000", "000001"]
    assert calls == [1, 2, 3]
