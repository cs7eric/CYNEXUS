from __future__ import annotations

from datetime import date

from cynexus_data.domain.stock.ths_industry_constituents import normalize_industry_constituent_snapshot


def test_normalize_industry_constituent_snapshot_pads_and_dedupes_codes() -> None:
    records = normalize_industry_constituent_snapshot(
        [
            {"序号": 1, "代码": 3030, "名称": "祖名股份", "现价": "23.42", "涨跌幅(%)": "10.01", "成交额": "0.97亿"},
            {"序号": 2, "代码": "003030", "名称": "祖名股份"},
            {"序号": 3, "代码": "600597", "名称": "光明乳业", "市盈率": "--"},
        ],
        industry_code="881103",
        snapshot_date=date(2026, 6, 26),
        source_type="json_import",
    )

    assert [record["stock_code"] for record in records] == ["003030", "600597"]
    assert records[0]["industry_code"] == "881103"
    assert records[0]["price"] == 23.42
    assert records[0]["change_pct"] == 10.01
    assert records[0]["amount_text"] == "0.97亿"
    assert records[1]["pe"] is None
    assert records[1]["source_type"] == "json_import"
