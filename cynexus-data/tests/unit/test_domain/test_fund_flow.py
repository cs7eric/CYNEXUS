from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.fund_flow import (
    normalize_sector_fund_flow_snapshot,
    normalize_stock_fund_flow_snapshot,
    sector_fund_flow_top_bottom,
)


def test_normalize_sector_fund_flow_snapshot_sorts_and_reranks() -> None:
    rows = [
        {"序号": 9, "行业": "银行", "净额(亿)": "-3", "行业指数涨跌幅": "-0.2%", "流入资金(亿)": "2", "流出资金(亿)": "5"},
        {"序号": 1, "行业": "软件服务", "净额(亿)": "6", "行业指数涨跌幅": "1.2%", "流入资金(亿)": "10", "流出资金(亿)": "4", "公司家数": "120"},
    ]

    records = normalize_sector_fund_flow_snapshot(rows, trade_date=date(2026, 6, 26))

    assert records[0]["sector_name"] == "软件服务"
    assert records[0]["rank"] == 1
    assert records[0]["net"] == 6.0
    assert records[0]["company_count"] == 120
    assert records[1]["rank"] == 2


def test_normalize_sector_fund_flow_snapshot_enriches_code_by_name() -> None:
    rows = [{"行业": "软件服务", "净额(亿)": "6"}]

    records = normalize_sector_fund_flow_snapshot(
        rows,
        trade_date=date(2026, 6, 26),
        sector_code_by_name={"软件服务": "881121"},
    )

    assert records[0]["sector_code"] == "881121"


def test_sector_fund_flow_top_bottom() -> None:
    records = [{"sector_name": "A"}, {"sector_name": "B"}, {"sector_name": "C"}]
    payload = sector_fund_flow_top_bottom(records, top_n=2)

    assert [row["sector_name"] for row in payload["top"]] == ["A", "B"]
    assert [row["sector_name"] for row in payload["bottom"]] == ["C", "B"]


def test_normalize_stock_fund_flow_snapshot_handles_legacy_keys() -> None:
    rows = [
        {
            "code": "600000",
            "name": "浦发银行",
            "lastPrice": 10,
            "outerDisc": 600,
            "insideDish": 400,
            "activeNetAmountWan": 0.2,
            "disk": {"buyBigRatio": 0.1, "sellBigRatio": 0.2},
        }
    ]

    records = normalize_stock_fund_flow_snapshot(rows, trade_date=date(2026, 6, 26), source_type="json_import")

    assert records[0]["code"] == "600000"
    assert records[0]["last_price"] == 10.0
    assert records[0]["outer_disc_lots"] == 600.0
    assert records[0]["buy_big_ratio"] == 0.1
    assert records[0]["source_type"] == "json_import"
